package xyz.jadonfowler.compiler.backend

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import xyz.jadonfowler.compiler.globalModules
import java.io.File

class LLVMBackend(module: Module) : Backend(module) {

    val llvmModule: LLVMModuleRef
    val context: LLVMContextRef
    private val namedValues: MutableMap<String, ValueContainer> = mutableMapOf()
    private val clazzTypes: MutableMap<Clazz, LLVMTypeRef>
    val builder: LLVMBuilderRef

    private val targetMachine: LLVMTargetMachineRef

    private var initialized = false

    fun init() {
        if (!initialized) {
            LLVMInitializeAllTargetInfos()
            LLVMInitializeAllTargets()
            LLVMInitializeAllTargetMCs()
            LLVMLinkInMCJIT()
            LLVMInitializeNativeAsmPrinter()
            LLVMInitializeNativeAsmParser()
            LLVMInitializeNativeDisassembler()
            LLVMInitializeNativeTarget()
            initialized = true
        }
    }

    init {
        init()
        clazzTypes = mutableMapOf()
        context = LLVMContextCreate()
        llvmModule = LLVMModuleCreateWithNameInContext(module.name, context)

        val targetTriple = LLVMGetDefaultTargetTriple()
        LLVMSetTarget(llvmModule, targetTriple)

        val target = LLVMTargetRef(null as BytePointer?)
        val error = BytePointer(null as Pointer?)
        LLVMGetTargetFromTriple(targetTriple, target, error)

        LLVMDisposeMessage(error)

        targetMachine = LLVMCreateTargetMachine(target, targetTriple.string, "", "", 0, 0, 0)
        LLVMDisposeMessage(targetTriple)

        builder = LLVMCreateBuilder()

        // Declare Malloc
        val mallocType: LLVMTypeRef = LLVMFunctionType(LLVMPointerType(LLVMInt8TypeInContext(context), 0),
                PointerPointer<LLVMTypeRef>(*arrayOf(LLVMInt64TypeInContext(context))), 1, 0)
        val malloc = LLVMAddFunction(llvmModule, "malloc", mallocType)
        namedValues.put("malloc", ValueContainer(ValueType.FUNCTION, malloc, T_INT8))

        // Declare Free
        val freeType: LLVMTypeRef = LLVMFunctionType(LLVMVoidTypeInContext(context),
                PointerPointer<LLVMTypeRef>(*arrayOf(LLVMPointerType(LLVMInt8TypeInContext(context), 0))), 1, 0)
        val free = LLVMAddFunction(llvmModule, "free", freeType)
        namedValues.put("free", ValueContainer(ValueType.FUNCTION, free, T_VOID))

        module.imports.forEach { visit(it) }
        module.globalVariables.forEach { visit(it) }
        module.globalClasses.forEach { visit(it) }
        module.globalFunctions.forEach { visit(it) }
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
        LLVMContextDispose(context)
        LLVMDisposePassManager(pass)
    }

    fun visit(import: Import) {
        val module = globalModules.filter { it.name == import.reference.name }.last()

        module.globalVariables.forEach {
            val global = LLVMAddGlobal(llvmModule, getLLVMType(it.type), it.name)
            if (it.constant) LLVMSetGlobalConstant(global, 1)
            LLVMSetExternallyInitialized(global, 1)
            namedValues.put(it.name, ValueContainer(ValueType.GLOBAL, global, it.type))
        }

        module.globalFunctions.forEach {
            val argumentTypes: List<LLVMTypeRef> = it.formals.map {
                if (it.type is Clazz) {
                    val classType = getLLVMType(it.type)
                    LLVMPointerType(classType, 0)
                } else getLLVMType(it.type)!!
            }

            // Get the FunctionType of the Function
            val returnType = if (it.returnType is Clazz) {
                // Return a pointer if the return type is a class
                val clazzType = getLLVMType(it.returnType)
                LLVMPointerType(clazzType, 0)
            } else getLLVMType(it.returnType)

            val llvmFunctionType: LLVMTypeRef = LLVMFunctionType(returnType,
                    PointerPointer<LLVMTypeRef>(*argumentTypes.toTypedArray()), argumentTypes.size, 0)

            val externAttribute: Attribute? = if (it.attributes.isEmpty()) null
            else it.attributes.filter { it.name == "extern" }.last()

            val llvmFunctionName = if (externAttribute != null) {
                if (externAttribute.values.isNotEmpty())
                    externAttribute.values[0]
                else it.name
            } else it.name

            val llvmFunction: LLVMValueRef = LLVMAddFunction(llvmModule, llvmFunctionName, llvmFunctionType)
            namedValues.put(it.name, ValueContainer(ValueType.FUNCTION, llvmFunction, it.returnType))
        }

        module.globalClasses.forEach {
            val clazz = it
            clazz.methods.map { methodToFunction(clazz, it) }.forEach {
                val argumentTypes: List<LLVMTypeRef> = it.formals.map {
                    if (it.type is Clazz) {
                        val classType = getLLVMType(it.type)
                        LLVMPointerType(classType, 0)
                    } else getLLVMType(it.type)!!
                }

                // Get the FunctionType of the Function
                val returnType = if (it.returnType is Clazz) {
                    // Return a pointer if the return type is a class
                    val clazzType = getLLVMType(it.returnType)
                    LLVMPointerType(clazzType, 0)
                } else getLLVMType(it.returnType)

                val llvmFunctionType: LLVMTypeRef = LLVMFunctionType(returnType,
                        PointerPointer<LLVMTypeRef>(*argumentTypes.toTypedArray()), argumentTypes.size, 0)

                val externAttribute: Attribute? = if (it.attributes.isEmpty()) null
                else it.attributes.filter { it.name == "extern" }.last()

                val llvmFunctionName = if (externAttribute != null) {
                    if (externAttribute.values.isNotEmpty())
                        externAttribute.values[0]
                    else it.name
                } else it.name

                val llvmFunction: LLVMValueRef = LLVMAddFunction(llvmModule, llvmFunctionName, llvmFunctionType)
                namedValues.put(it.name, ValueContainer(ValueType.FUNCTION, llvmFunction, it.returnType))
            }
        }
    }

    fun methodToFunction(clazz: Clazz, method: Function): Function {
        val function = method.copy()
        val formals = function.formals.toMutableList()
        formals.add(0, Formal(clazz, "_" + clazz.name))
        function.formals = formals
        function.name = clazz.name + "_" + method.name
        return function
    }

    override fun visit(clazz: Clazz) {
        if (clazz.constructor != null)
            visit(methodToFunction(clazz, clazz.constructor), namedValues, clazz)
        clazz.methods.map { methodToFunction(clazz, it) }.forEach { visit(it, namedValues, clazz) }
    }

    override fun visit(variable: Variable) {
        val global = LLVMAddGlobal(llvmModule, getLLVMType(variable.type), variable.name)
        if (variable.initialExpression != null) {
            LLVMSetInitializer(global, visit(variable.initialExpression!!, builder, mutableMapOf(), null, null, mutableListOf()))
        }
        if (variable.constant) LLVMSetGlobalConstant(global, 1)
        namedValues.put(variable.name, ValueContainer(ValueType.GLOBAL, global, variable.type))
    }

    override fun visit(function: Function) {
        visit(function, namedValues)
    }

    fun visit(function: Function, localVariables: MutableMap<String, ValueContainer>, clazz: Clazz? = null) {
        val externAttribute: Attribute? = if (function.attributes.isEmpty()) null
        else function.attributes.filter { it.name == "extern" }.last()

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

        val llvmFunctionName = if (externAttribute != null) {
            if (externAttribute.values.isNotEmpty())
                externAttribute.values[0]
            else function.name
        } else function.name

        val llvmFunction: LLVMValueRef = LLVMAddFunction(llvmModule, llvmFunctionName, llvmFunctionType)
        namedValues.put(function.name, ValueContainer(ValueType.FUNCTION, llvmFunction, function.returnType))

        // Don't build the function if it is externally defined
        if (externAttribute != null) return

        LLVMSetFunctionCallConv(llvmFunction, LLVMCCallConv)

        val entryBlock = LLVMAppendBasicBlock(llvmFunction, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)

        // Add formals to LVT
        function.formals.forEachIndexed { i, formal ->
            localVariables.put(formal.name, ValueContainer(ValueType.CONSTANT, LLVMGetParam(llvmFunction, i), formal.type))
        }

        // Add class fields to LVT
        if (clazz != null) {
            clazz.fields.forEach {
                localVariables.put(it.name, ValueContainer(ValueType.FIELD, null, it.type))
            }
        }

        // Classes to free
        val allocatedClasses: MutableList<TypeContainer> = mutableListOf()

        // Build the statements
        function.statements.forEach { visit(it, builder, llvmFunction, localVariables, clazz, allocatedClasses) }

        val returnName = if (function.expression != null && function.expression is ReferenceExpression)
            (function.expression as ReferenceExpression).reference.name
        else ""

        // Free the allocated classes
        allocatedClasses.forEach {
            if (returnName != it.name) {
                val thingsToFree: MutableMap<LLVMValueRef?, String> = mutableMapOf()

                // This has to be a Clazz
                val clazzType = it.type as Clazz

                // Check if fields need to be free
                clazzType.fields.forEachIndexed { i, variable ->
                    if (variable.type is Clazz)
                    // Their is a field that we need to free
                        thingsToFree.put(
                                // Load the pointer
                                LLVMBuildLoad(builder,
                                        LLVMBuildStructGEP(builder, it.value, i, variable.name),
                                        "load.${variable.name}"),
                                variable.name)
                }
                // Add the parent
                thingsToFree.put(it.value, it.name)

                // Call the free function for each pointer
                thingsToFree.forEach {
                    val bitPointer = LLVMBuildBitCast(builder, it.key,
                            LLVMPointerType(LLVMInt8TypeInContext(context), 0), "bitPointerTo${it.value}")
                    LLVMBuildCall(builder, namedValues["free"]!!.llvmValueRef,
                            PointerPointer<LLVMValueRef>(*arrayOf(bitPointer)), 1, "")
                }
            }
        }

        // Add a return statement with the last expression
        if (function.expression != null) {
            val ret_value = visit(function.expression!!, builder, localVariables, clazz, llvmFunction, allocatedClasses)
            LLVMBuildRet(builder, ret_value)
        } else
            LLVMBuildRetVoid(builder)
    }

    fun visit(statement: Statement, builder: LLVMBuilderRef, llvmFunction: LLVMValueRef,
              localVariables: MutableMap<String, ValueContainer>, clazz: Clazz?,
              allocatedClasses: MutableList<TypeContainer>) {
        when (statement) {
            is VariableDeclarationStatement -> {
                val variable = statement.variable
                if (variable.initialExpression != null) {
                    if (variable.constant) {
                        val value = visit(variable.initialExpression!!, builder, localVariables, clazz, llvmFunction, allocatedClasses)

                        // Check if we need to free this later
                        if (variable.initialExpression is ClazzInitializerExpression)
                            allocatedClasses.add(TypeContainer(variable.name, value, variable.type))
                        else if (variable.initialExpression is FunctionCallExpression) {
                            val functionReference = (variable.initialExpression as FunctionCallExpression).functionCall.functionReference
                            val function = module.getFunctionFromReference(functionReference)
                            if (function != null && function.returnType is Clazz)
                                allocatedClasses.add(TypeContainer(variable.name, value, variable.type))
                        }

                        localVariables.put(variable.name, ValueContainer(ValueType.CONSTANT, value, variable.type))
                    } else {
                        // Allocate memory so we can modify the value later
                        val allocation = LLVMBuildAlloca(builder, getLLVMType(variable.type), variable.name)
                        val value = visit(variable.initialExpression!!, builder, localVariables, clazz, llvmFunction, allocatedClasses)
                        LLVMBuildStore(builder, value, allocation)
                        localVariables.put(variable.name, ValueContainer(ValueType.ALLOCATION, allocation, variable.type))
                    }
                }
            }
            is VariableReassignmentStatement -> {
                val variable = localVariables[statement.reference.name]!!
                val value = visit(statement.expression, builder, localVariables, clazz, llvmFunction, allocatedClasses)

                // Check if we need to free this later
                if (statement.expression is ClazzInitializerExpression)
                    allocatedClasses.add(TypeContainer(statement.reference.name, value, variable.type))
                else if (statement.expression is FunctionCallExpression) {
                    // If the function returns a Class, we need to deallocate it once our scope ends
                    val functionReference = (statement.expression as FunctionCallExpression).functionCall.functionReference
                    val function = module.getFunctionFromReference(functionReference)
                    if (function != null && function.returnType is Clazz)
                        allocatedClasses.add(TypeContainer(statement.reference.name, value, variable.type))
                }

                if (variable.valueType == ValueType.FIELD && clazz != null) {
                    // reassigning field from method
                    val clazzInstance = LLVMGetFirstParam(llvmFunction)
                    val indexInClass = clazz.fields.map { it.name }.indexOf(statement.reference.name)
                    LLVMBuildStore(builder, value,
                            LLVMBuildStructGEP(builder, clazzInstance, indexInClass, statement.reference.name))
                } else {
                    LLVMBuildStore(builder, value, variable.llvmValueRef)
                }
            }
            is IfStatement -> {
                val condition = visit(statement.expression, builder, localVariables, clazz, llvmFunction, allocatedClasses)

                // Add blocks for True & False, and another one where they merge
                val trueBlock = LLVMAppendBasicBlock(llvmFunction, "if.t ${statement.expression}")
                val falseBlock = LLVMAppendBasicBlock(llvmFunction, "if.f ${statement.expression}")
                val mergeBlock = LLVMAppendBasicBlock(llvmFunction, "if.o ${statement.expression}")

                LLVMBuildCondBr(builder, condition, trueBlock, falseBlock)

                // Visit each branch and tell them to merge afterwards
                LLVMPositionBuilderAtEnd(builder, trueBlock)

                val allocatedClassesInIf: MutableList<TypeContainer> = mutableListOf()
                statement.statements.forEach { visit(it, builder, llvmFunction, localVariables, clazz, allocatedClassesInIf) }

                // Free the allocated classes
                allocatedClassesInIf.forEach {
                    val thingsToFree: MutableMap<LLVMValueRef?, String> = mutableMapOf()

                    // This has to be a Clazz
                    val clazzType = it.type as Clazz

                    // Check if fields need to be free
                    clazzType.fields.forEachIndexed { i, variable ->
                        if (variable.type is Clazz)
                        // Their is a field that we need to free
                            thingsToFree.put(
                                    // Load the pointer
                                    LLVMBuildLoad(builder,
                                            LLVMBuildStructGEP(builder, it.value, i, variable.name),
                                            "load.${variable.name}"),
                                    variable.name)
                    }
                    // Add the parent
                    thingsToFree.put(it.value, it.name)

                    // Call the free function for each pointer
                    thingsToFree.forEach {
                        val bitPointer = LLVMBuildBitCast(builder, it.key,
                                LLVMPointerType(LLVMInt8TypeInContext(context), 0), "bitPointerTo${it.value}")
                        LLVMBuildCall(builder, namedValues["free"]!!.llvmValueRef,
                                PointerPointer<LLVMValueRef>(*arrayOf(bitPointer)), 1, "")
                    }
                }

                LLVMBuildBr(builder, mergeBlock)

                LLVMPositionBuilderAtEnd(builder, falseBlock)
                if (statement.elseStatement != null)
                    visit(statement.elseStatement!!, builder, llvmFunction, localVariables, clazz, allocatedClasses)
                LLVMBuildBr(builder, mergeBlock)

                LLVMPositionBuilderAtEnd(builder, mergeBlock)
            }
            is WhileStatement -> {
                val whileCondition = LLVMAppendBasicBlock(llvmFunction, "while.c ${statement.expression}")
                val whileBlock = LLVMAppendBasicBlock(llvmFunction, "while.b ${statement.expression}")
                val outside = LLVMAppendBasicBlock(llvmFunction, "while.o ${statement.expression}")

                LLVMBuildBr(builder, whileCondition)

                LLVMPositionBuilderAtEnd(builder, whileCondition)
                val condition = visit(statement.expression, builder, localVariables, clazz, llvmFunction, allocatedClasses)
                LLVMBuildCondBr(builder, condition, whileBlock, outside)

                LLVMPositionBuilderAtEnd(builder, whileBlock)

                val allocatedClassesInLoop: MutableList<TypeContainer> = mutableListOf()
                statement.statements.forEach { visit(it, builder, llvmFunction, localVariables, clazz, allocatedClassesInLoop) }

                // Free the allocated classes
                allocatedClassesInLoop.forEach {
                    val thingsToFree: MutableMap<LLVMValueRef?, String> = mutableMapOf()

                    // This has to be a Clazz
                    val clazzType = it.type as Clazz

                    // Check if fields need to be free
                    clazzType.fields.forEachIndexed { i, variable ->
                        if (variable.type is Clazz)
                        // Their is a field that we need to free
                            thingsToFree.put(
                                    // Load the pointer
                                    LLVMBuildLoad(builder,
                                            LLVMBuildStructGEP(builder, it.value, i, variable.name),
                                            "load.${variable.name}"),
                                    variable.name)
                    }
                    // Add the parent
                    thingsToFree.put(it.value, it.name)

                    // Call the free function for each pointer
                    thingsToFree.forEach {
                        val bitPointer = LLVMBuildBitCast(builder, it.key,
                                LLVMPointerType(LLVMInt8TypeInContext(context), 0), "bitPointerTo${it.value}")
                        LLVMBuildCall(builder, namedValues["free"]!!.llvmValueRef,
                                PointerPointer<LLVMValueRef>(*arrayOf(bitPointer)), 1, "")
                    }
                }

                LLVMBuildBr(builder, whileCondition)

                LLVMPositionBuilderAtEnd(builder, outside)
            }
            is FunctionCallStatement -> {
                val functionCall = statement.functionCall
                val function = namedValues.filter { it.key == functionCall.functionReference.name }
                        .values.filter { it.valueType == ValueType.FUNCTION }.last()
                val expressions = functionCall.arguments.map { visit(it, builder, localVariables, clazz, llvmFunction, allocatedClasses) }
                LLVMBuildCall(builder, function.llvmValueRef, PointerPointer(*expressions.toTypedArray()),
                        functionCall.arguments.size, if (function.type == T_VOID) "" else statement.functionCall.toString())
            }
            is MethodCallStatement -> {
                val methodCall = statement.methodCall
                val variable = visit(ReferenceExpression(methodCall.variableReference), builder, localVariables, clazz, llvmFunction, allocatedClasses)
                val clazzName = (localVariables[methodCall.variableReference.name]?.type as? Clazz)?.name ?: "null"
                val functionName = clazzName + "_" + methodCall.methodReference.name
                val function = namedValues.filter { it.key == functionName }
                        .values.filter { it.valueType == ValueType.FUNCTION }.last()
                val expressions = methodCall.arguments.map { visit(it, builder, localVariables, clazz, llvmFunction, allocatedClasses) }.toMutableList()
                expressions.add(0, variable)
                LLVMBuildCall(builder, function.llvmValueRef, PointerPointer(*expressions.toTypedArray()),
                        methodCall.arguments.size + 1, if (function.type == T_VOID) "" else methodCall.toString())
            }
            is FieldSetterStatement -> {
                val variable = visit(statement.variable, builder, localVariables, clazz, llvmFunction, allocatedClasses)
                val value = visit(statement.expression, builder, localVariables, clazz, llvmFunction, allocatedClasses)

                // Don't free class at the end of a function
                val expression = statement.expression
                if (expression is ReferenceExpression) {
                    val name = expression.reference.name
                    allocatedClasses.forEach {
                        if (it.name == name)
                            allocatedClasses.remove(it)
                    }
                }

                val type = getType(variable)
                val indexInClass = (type as Clazz).fields
                        .map { it.name }.indexOf(statement.fieldReference.name)
                LLVMBuildStore(builder,
                        value,
                        LLVMBuildStructGEP(builder, variable, indexInClass, statement.toString()))
            }
        }
    }

    fun visit(expression: Expression, builder: LLVMBuilderRef, localVariables: MutableMap<String, ValueContainer>,
              clazz: Clazz?, function: LLVMValueRef?, allocatedClasses: MutableList<TypeContainer>): LLVMValueRef {
        return when (expression) {
            is IntegerLiteral -> LLVMConstInt(LLVMInt32TypeInContext(context), expression.value.toLong(), 0)
            is FloatLiteral -> LLVMConstReal(getLLVMType(expression.type), expression.value)
            is BooleanExpression -> {
                if (expression.value) LLVMConstInt(LLVMInt1TypeInContext(context), 1, 0)
                else LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0)
            }
            is BinaryOperator -> {
                var A = visit(expression.expressionA, builder, localVariables, clazz, function, allocatedClasses)
                var B = visit(expression.expressionB, builder, localVariables, clazz, function, allocatedClasses)

                if (isLLVMType(A, LLVMIntegerTypeKind) && isLLVMType(B, LLVMIntegerTypeKind)) {
                    // Higher bit integer types are declared after the lower ones,
                    // so we can compare the addresses of the types
                    val aType = LLVMTypeOf(A)
                    val bType = LLVMTypeOf(B)
                    if (aType.address() > bType.address()) {
                        // a has more bits
                        B = LLVMBuildZExt(builder, B, aType, expression.expressionB.toString() + " extended")
                    } else {
                        // b has more bits
                        A = LLVMBuildZExt(builder, A, bType, expression.expressionA.toString() + " extended")
                    }
                } else if (isLLVMType(A, LLVMFloatTypeKind, LLVMDoubleTypeKind, LLVMFP128TypeKind)
                        && isLLVMType(B, LLVMFloatTypeKind, LLVMDoubleTypeKind, LLVMFP128TypeKind)) {
                    val aType = LLVMTypeOf(A)
                    val bType = LLVMTypeOf(B)

                    if (aType.address() > bType.address()) {
                        // a has more bits
                        B = LLVMBuildFPExt(builder, B, aType, expression.expressionB.toString() + " extended")
                    } else {
                        // b has more bits
                        A = LLVMBuildFPExt(builder, A, bType, expression.expressionA.toString() + " extended")
                    }
                }

                when (expression.operator) {
                    Operator.PLUS_INT -> LLVMBuildAdd(builder, A, B, expression.toString())
                    Operator.MINUS_INT -> LLVMBuildSub(builder, A, B, expression.toString())
                    Operator.MULTIPLY_INT -> LLVMBuildMul(builder, A, B, expression.toString())
                    Operator.DIVIDE_INT -> LLVMBuildSDiv(builder, A, B, expression.toString())
                    Operator.PLUS_FLOAT -> LLVMBuildFAdd(builder, A, B, expression.toString())
                    Operator.MINUS_FLOAT -> LLVMBuildFSub(builder, A, B, expression.toString())
                    Operator.MULTIPLY_FLOAT -> LLVMBuildFMul(builder, A, B, expression.toString())
                    Operator.DIVIDE_FLOAT -> LLVMBuildFDiv(builder, A, B, expression.toString())
                    Operator.EQUALS -> LLVMBuildICmp(builder, LLVMIntEQ, A, B, expression.toString())
                    Operator.NOT_EQUAL -> LLVMBuildICmp(builder, LLVMIntNE, A, B, expression.toString())
                    Operator.GREATER_THAN -> LLVMBuildICmp(builder, LLVMIntSGT, A, B, expression.toString())
                    Operator.GREATER_THAN_EQUAL -> LLVMBuildICmp(builder, LLVMIntSGE, A, B, expression.toString())
                    Operator.LESS_THAN -> LLVMBuildICmp(builder, LLVMIntSLT, A, B, expression.toString())
                    Operator.LESS_THAN_EQUAL -> LLVMBuildICmp(builder, LLVMIntSLE, A, B, expression.toString())
                    else -> LLVMConstInt(LLVMInt32TypeInContext(context), 0, 0)
                }
            }
            is ReferenceExpression -> {
                val ref = expression.reference.name
                if (localVariables.containsKey(ref)) {
                    val value = localVariables[ref]!!
                    if (value.valueType == ValueType.ALLOCATION || value.valueType == ValueType.GLOBAL) {
                        LLVMBuildLoad(builder, value.llvmValueRef, ref)
                    } else if (value.valueType == ValueType.FIELD && clazz != null && function != null) {
                        val variable = LLVMGetFirstParam(function)
                        val indexInClass = clazz.fields.map { it.name }.indexOf(ref)
                        LLVMBuildLoad(builder,
                                LLVMBuildStructGEP(builder, variable, indexInClass, ref),
                                expression.toString())
                    } else value.llvmValueRef!!
                } else throw Exception("Can't find reference: $expression.")
            }
            is FunctionCallExpression -> {
                val functionCall = expression.functionCall
                val llvmFunction = namedValues.filter { it.key == functionCall.functionReference.name }
                        .values.filter { it.valueType == ValueType.FUNCTION }.last()
                val expressions = functionCall.arguments.map { visit(it, builder, localVariables, clazz, function, allocatedClasses) }
                LLVMBuildCall(builder, llvmFunction.llvmValueRef, PointerPointer(*expressions.toTypedArray()),
                        functionCall.arguments.size, functionCall.toString())
            }
            is MethodCallExpression -> {
                val methodCall = expression.methodCall
                val variable = visit(ReferenceExpression(methodCall.variableReference), builder, localVariables, clazz, function, allocatedClasses)
                val clazzName = (localVariables[methodCall.variableReference.name]?.type as? Clazz)?.name ?: "null"
                val functionName = clazzName + "_" + methodCall.methodReference.name
                val method = namedValues.filter { it.key == functionName }
                        .values.filter { it.valueType == ValueType.FUNCTION }.last()
                val expressions = methodCall.arguments.map { visit(it, builder, localVariables, clazz, function, allocatedClasses) }.toMutableList()
                expressions.add(0, variable)
                LLVMBuildCall(builder, method.llvmValueRef, PointerPointer(*expressions.toTypedArray()),
                        methodCall.arguments.size + 1, methodCall.toString())
            }
            is ClazzInitializerExpression -> {
                val clazzOfExpression = module.getClazzFromReference(expression.classReference)
                if (clazzOfExpression != null) {
                    // Size of Class in Bytes
                    fun sizeOfClazz(clazz: Clazz): Long = clazz.fields.map {
                        val type = it.type
                        when (type) {
                            T_BOOL -> 1L
                            T_INT8 -> 1L
                            T_INT16 -> 2L
                            T_INT32 -> 4L
                            T_INT64 -> 8L
                            T_INT128 -> 16L
                            T_FLOAT32 -> 4L
                            T_FLOAT64 -> 8L
                            T_FLOAT128 -> 16L
                            is Clazz -> 4L // Size of a pointer
                            else -> 4L
                        }
                    }.sum()

                    val size = sizeOfClazz(clazzOfExpression)
                    // Call malloc
                    val mallocMemory = LLVMBuildCall(builder, namedValues["malloc"]!!.llvmValueRef,
                            PointerPointer<LLVMValueRef>(*arrayOf(LLVMConstInt(LLVMInt64TypeInContext(context), size, 0))),
                            1, "malloc($size) for ${clazzOfExpression.name}")
                    // Cast the i8* that malloc returns to a pointer of the Class we want
                    val clazzValue = LLVMBuildBitCast(builder, mallocMemory,
                            LLVMPointerType(getLLVMType(clazzOfExpression), 0), "castTo${clazzOfExpression.name}")

                    clazzOfExpression.fields.forEachIndexed { i, variable ->
                        if (variable.initialExpression != null) {
                            // Where the field is in the struct
                            val fieldPointer = LLVMBuildStructGEP(builder, clazzValue, i, variable.name)
                            // Store the value
                            val fieldValue = visit(variable.initialExpression!!, builder, mutableMapOf(), null, null, mutableListOf())
                            LLVMBuildStore(builder, fieldValue, fieldPointer)
                        }
                    }

                    // Call constructor
                    if (clazzOfExpression.constructor != null) {
                        // Find constructor
                        val functionName = clazzOfExpression.name + "_init"
                        val method = namedValues.filter { it.key == functionName }
                                .values.filter { it.valueType == ValueType.FUNCTION }.last()

                        // Build arguments
                        val arguments = expression.arguments.map {
                            val e = visit(it, builder, localVariables, null, null, allocatedClasses)
                            // Don't free class at the end of a function
                            if (it is ReferenceExpression) {
                                val name = it.reference.name
                                val allocationsToRemove = mutableListOf<TypeContainer>()
                                allocatedClasses.forEach {
                                    if (it.name == name)
                                        allocationsToRemove.add(it)
                                }
                                allocationsToRemove.forEach {
                                    allocatedClasses.remove(it)
                                }
                            }
                            e
                        }.toMutableList()
                        arguments.add(0, clazzValue)

                        LLVMBuildCall(builder, method.llvmValueRef, PointerPointer(*arguments.toTypedArray()),
                                arguments.size, if (method.type == T_VOID) ""
                        else "$ { clazzOfExpression.name }_init(${expression.arguments})")
                    }

                    // Return the original Class allocation
                    clazzValue
                } else throw Exception("Unimplemented ${expression.javaClass.simpleName}: $expression.")
            }
            is FieldGetterExpression -> {
                val variable = visit(expression.variable, builder, localVariables, clazz, function, allocatedClasses)
                val type = getType(variable)
                val indexInClass = (type as Clazz).fields
                        .map { it.name }.indexOf(expression.fieldReference.name)
                LLVMBuildLoad(builder,
                        LLVMBuildStructGEP(builder, variable, indexInClass, expression.variable.toString()),
                        expression.toString())
            }
            else -> throw Exception("Unimplemented ${expression.javaClass.simpleName}: $expression.")
        }
    }

    fun getLLVMType(type: Type): LLVMTypeRef? {
        return when (type) {
            T_BOOL -> LLVMInt1TypeInContext(context)
            T_INT8 -> LLVMInt8TypeInContext(context)
            T_INT16 -> LLVMInt16TypeInContext(context)
            T_INT32 -> LLVMInt32TypeInContext(context)
            T_INT64 -> LLVMInt64TypeInContext(context)
            T_INT128 -> LLVMInt128TypeInContext(context)
            T_FLOAT32 -> LLVMFloatTypeInContext(context)
            T_FLOAT64 -> LLVMDoubleTypeInContext(context)
            T_FLOAT128 -> LLVMFP128TypeInContext(context)
            T_VOID -> LLVMVoidTypeInContext(context)
            is Clazz -> {
                if (!clazzTypes.containsKey(type)) {
                    val fieldTypes = type.fields.map {
                        if (it.type is Clazz) LLVMPointerType(getLLVMType(it.type), 0)
                        else getLLVMType(it.type)
                    }
                    val llvmClazzType = LLVMStructCreateNamed(context, type.name)
                    LLVMStructSetBody(llvmClazzType, PointerPointer(*fieldTypes.toTypedArray()), type.fields.size, 0)
                    clazzTypes.put(type, llvmClazzType)
                }
                clazzTypes[type]
            }
            T_UNDEF -> {
                throw Exception("Can't find LLVM type for Undefined!")
            }
            else -> null
        }
    }

    fun getType(v: LLVMValueRef): Type {
        return getType(LLVMTypeOf(v))
    }

    fun getType(type: LLVMTypeRef): Type {
        return when (type) {
            LLVMInt1TypeInContext(context) -> T_BOOL
            LLVMInt8TypeInContext(context) -> T_INT8
            LLVMInt16TypeInContext(context) -> T_INT16
            LLVMInt32TypeInContext(context) -> T_INT32
            LLVMInt64TypeInContext(context) -> T_INT64
            LLVMInt128TypeInContext(context) -> T_INT128
            LLVMFloatTypeInContext(context) -> T_FLOAT32
            LLVMDoubleTypeInContext(context) -> T_FLOAT64
            LLVMFP128TypeInContext(context) -> T_FLOAT128
            LLVMVoidTypeInContext(context) -> T_VOID
            else -> {
                clazzTypes.forEach {
                    if (LLVMPointerType(it.value, 0) == type) {
                        return it.key
                    }
                }
                T_UNDEF
            }
        }
    }

    fun isLLVMType(v: LLVMValueRef, vararg t: Int): Boolean {
        val typeKind = LLVMGetTypeKind(LLVMTypeOf(v))
        t.forEach { if (it == typeKind) return true }
        return false
    }

    enum class ValueType {
        FUNCTION,
        CONSTANT,
        ALLOCATION,
        GLOBAL,
        FIELD,
    }

    class ValueContainer(val valueType: ValueType, val llvmValueRef: LLVMValueRef?, val type: Type)

    class TypeContainer(val name: String, val value: LLVMValueRef?, val type: Type)

}
