package xyz.jadonfowler.compiler.backend

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import xyz.jadonfowler.compiler.globalModules
import java.io.File

class LLVMBackend(module: Module) : Backend(module) {

    val llvmModule: LLVMModuleRef
    val namedValues: MutableMap<String, ValueContainer> = mutableMapOf()
    val builder: LLVMBuilderRef

    private val targetMachine: LLVMTargetMachineRef

    init {
        LLVMInitializeAllTargetInfos()
        LLVMInitializeAllTargets()
        LLVMInitializeAllTargetMCs()
        LLVMLinkInMCJIT()
        LLVMInitializeNativeAsmPrinter()
        LLVMInitializeNativeAsmParser()
        LLVMInitializeNativeDisassembler()
        LLVMInitializeNativeTarget()

        llvmModule = LLVMModuleCreateWithName(module.name)

        val targetTriple = LLVMGetDefaultTargetTriple()
        LLVMSetTarget(llvmModule, targetTriple)

        val target = LLVMTargetRef(null as BytePointer?)
        val error = BytePointer(null as Pointer?)
        LLVMGetTargetFromTriple(targetTriple, target, error)

        LLVMDisposeMessage(error)

        targetMachine = LLVMCreateTargetMachine(target, targetTriple.string, "", "", 0, 0, 0)
        LLVMDisposeMessage(targetTriple)

        builder = LLVMCreateBuilder()

        module.imports.forEach { visit(it) }
        module.globalVariables.forEach { it.accept(this) }
        module.globalFunctions.forEach { it.accept(this) }
        module.globalClasses.forEach { it.accept(this) }
    }

    override fun output(file: File?) {
        var error = BytePointer(null as Pointer?)
        LLVMVerifyModule(llvmModule, LLVMAbortProcessAction, error)
        LLVMDisposeMessage(error)

        val pass = LLVMCreatePassManager()
        LLVMAddConstantPropagationPass(pass)
        LLVMAddInstructionCombiningPass(pass)
        LLVMAddPromoteMemoryToRegisterPass(pass)
        LLVMAddGVNPass(pass)
        LLVMAddCFGSimplificationPass(pass)
        LLVMRunPassManager(pass, llvmModule)

        if (file != null) {
            // Print out Assembly
            error = BytePointer(null as Pointer?)
            LLVMTargetMachineEmitToFile(targetMachine, llvmModule, BytePointer(file.path + ".s"), LLVMAssemblyFile, error)
            LLVMDisposeMessage(error)

            // Print out Object code
            error = BytePointer(null as Pointer?)
            LLVMTargetMachineEmitToFile(targetMachine, llvmModule, BytePointer(file.path + ".o"), LLVMObjectFile, error)
            LLVMDisposeMessage(error)

            // Print out LLVM IR
            error = BytePointer(null as Pointer?)
            LLVMPrintModuleToFile(llvmModule, file.path + ".ll", error)
            LLVMDisposeMessage(error)
        }

        LLVMDisposeBuilder(builder)
        LLVMDisposePassManager(pass)
    }

    fun visit(import: Import) {
        val module = globalModules.filter { it.name == import.reference.name }.last()
        module.globalFunctions.forEach {
            // Create the function
            val argumentTypes: List<LLVMTypeRef> = it.formals.map { getLLVMType(it.type)!! }
            val llvmFunctionType: LLVMTypeRef = LLVMFunctionType(getLLVMType(it.returnType),
                    PointerPointer<LLVMTypeRef>(*argumentTypes.toTypedArray()), argumentTypes.size, 0)
            val llvmFunction: LLVMValueRef = LLVMAddFunction(llvmModule, it.name, llvmFunctionType)
            namedValues.put(it.name, ValueContainer(ValueType.FUNCTION, llvmFunction, it.returnType))
        }
    }

    fun methodToFunction(clazz: Clazz, method: Function): Function {
        val function = method
        val formals = function.formals.toMutableList()
        formals.add(0, Formal(clazz, "_" + clazz.name))
        function.formals = formals
        return function
    }

    override fun visit(clazz: Clazz) {
        val localVariables: MutableMap<String, ValueContainer> = mutableMapOf()
        clazz.methods.map { methodToFunction(clazz, it) }.forEach { it.accept(this) }
    }


    override fun visit(variable: Variable) {
        val global = LLVMAddGlobal(llvmModule, getLLVMType(variable.type), variable.name)
        if (variable.initialExpression != null) {
            val builder = LLVMCreateBuilder()
            LLVMSetInitializer(global, visit(variable.initialExpression!!, builder, mutableMapOf()))
        }
        if (variable.constant) LLVMSetGlobalConstant(global, 1)
        namedValues.put(variable.name, ValueContainer(ValueType.GLOBAL, global, variable.type))
    }

    override fun visit(function: Function) {
        visit(function, namedValues)
    }

    fun visit(function: Function, localVariables: MutableMap<String, ValueContainer>) {
        // Get LLVM Types of Arguments
        val argumentTypes: List<LLVMTypeRef> = function.formals.map {
            if (it.type is Clazz) {
                val classType = getLLVMType(it.type)
                LLVMPointerType(classType, 0)
            } else getLLVMType(it.type)!!
        }
        // Get the FunctionType of the Function
        val returnType = if (function.returnType is Clazz) {
            // Return a pointer if the return type is a class
            val clazzType = getLLVMType(function.returnType)
            LLVMPointerType(clazzType, 0)
        } else getLLVMType(function.returnType)
        val llvmFunctionType: LLVMTypeRef = LLVMFunctionType(returnType,
                PointerPointer<LLVMTypeRef>(*argumentTypes.toTypedArray()), argumentTypes.size, 0)
        // Add the Function to the Module
        val llvmFunction: LLVMValueRef = LLVMAddFunction(llvmModule, function.name, llvmFunctionType)
        namedValues.put(function.name, ValueContainer(ValueType.FUNCTION, llvmFunction, function.returnType))

        // Don't build the function if it is externally defined
        if (function.attributes.map { it.name }.contains("extern")) return

        LLVMSetFunctionCallConv(llvmFunction, LLVMCCallConv)

        val entryBlock = LLVMAppendBasicBlock(llvmFunction, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)

        // Add formals to LVT
        var formalIndex = 0
        function.formals.forEach { localVariables.put(it.name, ValueContainer(ValueType.CONSTANT, LLVMGetParam(llvmFunction, formalIndex), it.type)); formalIndex++ }

        // Build the statements
        function.statements.forEach { visit(it, builder, llvmFunction, localVariables) }

        // Add a return statement with the last expression
        if (function.expression != null) {
            val ret_value = visit(function.expression!!, builder, localVariables)
            LLVMBuildRet(builder, ret_value)
        }
    }

    fun visit(statement: Statement, builder: LLVMBuilderRef, llvmFunction: LLVMValueRef, localVariables: MutableMap<String, ValueContainer>) {
        when (statement) {
            is VariableDeclarationStatement -> {
                val variable = statement.variable
                if (variable.initialExpression != null) {
                    if (variable.constant) {
                        localVariables.put(variable.name, ValueContainer(ValueType.CONSTANT, visit(variable.initialExpression!!, builder, localVariables), variable.type))
                    } else {
                        // Allocate memory so we can modify the value later
                        val allocation = LLVMBuildAlloca(builder, getLLVMType(variable.type), variable.name)
                        val value = visit(variable.initialExpression!!, builder, localVariables)
                        LLVMBuildStore(builder, value, allocation)
                        localVariables.put(variable.name, ValueContainer(ValueType.ALLOCATION, allocation, variable.type))
                    }
                }
            }
            is VariableReassignmentStatement -> {
                val variable = localVariables[statement.reference.name]!!
                val value = visit(statement.exp, builder, localVariables)
                LLVMBuildStore(builder, value, variable.llvmValueRef)
            }
            is IfStatement -> {
                val condition = visit(statement.exp, builder, localVariables)

                // Add blocks for True & False, and another one where they merge
                val trueBlock = LLVMAppendBasicBlock(llvmFunction, "if.t ${statement.exp}")
                val falseBlock = LLVMAppendBasicBlock(llvmFunction, "if.f ${statement.exp}")
                val mergeBlock = LLVMAppendBasicBlock(llvmFunction, "if.o ${statement.exp}")

                LLVMBuildCondBr(builder, condition, trueBlock, falseBlock)

                // Visit each branch and tell them to merge afterwards
                LLVMPositionBuilderAtEnd(builder, trueBlock)
                statement.statements.forEach { visit(it, builder, llvmFunction, localVariables) }
                LLVMBuildBr(builder, mergeBlock)

                LLVMPositionBuilderAtEnd(builder, falseBlock)
                if (statement.elseStatement != null)
                    visit(statement.elseStatement, builder, llvmFunction, localVariables)
                LLVMBuildBr(builder, mergeBlock)

                LLVMPositionBuilderAtEnd(builder, mergeBlock)
            }
            is WhileStatement -> {
                val whileCondition = LLVMAppendBasicBlock(llvmFunction, "while.c ${statement.exp}")
                val whileBlock = LLVMAppendBasicBlock(llvmFunction, "while.b ${statement.exp}")
                val outside = LLVMAppendBasicBlock(llvmFunction, "while.o ${statement.exp}")

                LLVMBuildBr(builder, whileCondition)

                LLVMPositionBuilderAtEnd(builder, whileCondition)
                val condition = visit(statement.exp, builder, localVariables)
                LLVMBuildCondBr(builder, condition, whileBlock, outside)

                LLVMPositionBuilderAtEnd(builder, whileBlock)
                statement.statements.forEach { visit(it, builder, llvmFunction, localVariables) }
                LLVMBuildBr(builder, whileCondition)

                LLVMPositionBuilderAtEnd(builder, outside)
            }
            is FunctionCallStatement -> {
                val functionCall = statement.functionCall
                val function = namedValues.filter { it.key == functionCall.functionReference.name }
                        .values.filter { it.valueType == ValueType.FUNCTION }.last()
                val expressions = functionCall.arguments.map { visit(it, builder, localVariables) }
                LLVMBuildCall(builder, function.llvmValueRef, PointerPointer(*expressions.toTypedArray()),
                        functionCall.arguments.size, statement.functionCall.toString())
            }
            is FieldSetterStatement -> {
                val variable = visit(ReferenceExpression(statement.variableReference), builder, localVariables)
                val indexInClass = (localVariables[statement.variableReference.name]?.type as Clazz).fields
                        .map { it.name }.indexOf(statement.fieldReference.name)
                LLVMBuildStore(builder,
                        visit(statement.expression, builder, localVariables),
                        LLVMBuildStructGEP(builder, variable, indexInClass, statement.toString()))
            }
        }
    }

    fun visit(expression: Expression, builder: LLVMBuilderRef, localVariables: MutableMap<String, ValueContainer>): LLVMValueRef {
        return when (expression) {
            is IntegerLiteral -> LLVMConstInt(LLVMInt32Type(), expression.value.toLong(), 0)
            is BooleanExpression -> {
                if (expression.value) LLVMConstInt(LLVMInt1Type(), 1, 0)
                else LLVMConstInt(LLVMInt1Type(), 0, 1)
            }
            is BinaryOperator -> {
                val A = visit(expression.expA, builder, localVariables)
                val B = visit(expression.expB, builder, localVariables)
                when (expression.operator) {
                    Operator.PLUS -> LLVMBuildAdd(builder, A, B, expression.toString())
                    Operator.MINUS -> LLVMBuildSub(builder, A, B, expression.toString())
                    Operator.MULTIPLY -> LLVMBuildMul(builder, A, B, expression.toString())
                    Operator.DIVIDE -> LLVMBuildSDiv(builder, A, B, expression.toString())
                    Operator.EQUALS -> LLVMBuildICmp(builder, LLVMIntEQ, A, B, expression.toString())
                    Operator.NOT_EQUAL -> LLVMBuildICmp(builder, LLVMIntNE, A, B, expression.toString())
                    Operator.GREATER_THAN -> LLVMBuildICmp(builder, LLVMIntSGT, A, B, expression.toString())
                    Operator.GREATER_THAN_EQUAL -> LLVMBuildICmp(builder, LLVMIntSGE, A, B, expression.toString())
                    Operator.LESS_THAN -> LLVMBuildICmp(builder, LLVMIntSLT, A, B, expression.toString())
                    Operator.LESS_THAN_EQUAL -> LLVMBuildICmp(builder, LLVMIntSLE, A, B, expression.toString())
                    else -> LLVMConstInt(LLVMInt32Type(), 0, 0)
                }
            }
            is ReferenceExpression -> {
                val ref = expression.reference.name
                if (localVariables.containsKey(ref)) {
                    val value = localVariables[ref]!!
                    if (value.valueType == ValueType.ALLOCATION) {
                        LLVMBuildLoad(builder, value.llvmValueRef, ref)
                    } else if (value.valueType == ValueType.GLOBAL) {
                        LLVMGetInitializer(value.llvmValueRef)
                    } else value.llvmValueRef
                } else LLVMConstInt(LLVMInt32Type(), 0, 0)
            }
            is FunctionCallExpression -> {
                val functionCall = expression.functionCall
                val function = namedValues.filter { it.key == functionCall.functionReference.name }
                        .values.filter { it.valueType == ValueType.FUNCTION }.last()
                val expressions = functionCall.arguments.map { visit(it, builder, localVariables) }
                LLVMBuildCall(builder, function.llvmValueRef, PointerPointer(*expressions.toTypedArray()),
                        functionCall.arguments.size, functionCall.toString())
            }
            is ClazzInitializerExpression -> {
                val clazz = module.getNodeFromReference(expression.classReference, null) as? Clazz
                if (clazz != null) {
                    LLVMBuildAlloca(builder, getLLVMType(clazz)!!, expression.toString())
                } else LLVMConstInt(LLVMInt32Type(), 0, 0)
            }
            is FieldGetterExpression -> {
                val variable = visit(ReferenceExpression(expression.variableReference), builder, localVariables)
                val indexInClass = (localVariables[expression.variableReference.name]?.type as Clazz).fields
                        .map { it.name }.indexOf(expression.fieldReference.name)
                LLVMBuildLoad(builder,
                        LLVMBuildStructGEP(builder, variable, indexInClass, expression.variableReference.name),
                        expression.toString())
            }
            else -> LLVMConstInt(LLVMInt32Type(), 0, 0)
        }
    }

    companion object {

        private val clazzTypes: MutableMap<String, LLVMTypeRef> = mutableMapOf()

        fun getLLVMType(type: Type): LLVMTypeRef? {
            return when (type) {
                T_BOOL -> LLVMInt1Type()
                T_INT -> LLVMInt32Type()
                T_VOID -> LLVMVoidType()
                is Clazz -> {
                    if (!clazzTypes.containsKey(type.name)) {
                        val fieldTypes = type.fields.map { getLLVMType(it.type) }
                        val llvmClazzType = LLVMStructCreateNamed(LLVMGetGlobalContext(), type.name)
                        LLVMStructSetBody(llvmClazzType, PointerPointer(*fieldTypes.toTypedArray()), type.fields.size, 0)
                        clazzTypes.put(type.name, llvmClazzType)
                    }
                    clazzTypes.get(type.name)
                }
                T_UNDEF -> {
                    throw Exception("Can't find LLVM type for Undefined!")
                }
                else -> null
            }
        }

    }

    enum class ValueType {
        FUNCTION,
        CONSTANT,
        ALLOCATION,
        GLOBAL,
    }

    class ValueContainer(val valueType: ValueType, val llvmValueRef: LLVMValueRef, val type: Type)

}
