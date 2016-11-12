package xyz.jadonfowler.compiler.backend

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import java.io.File

class LLVMBackend(module: Module) : Backend(module) {

    val llvmModule: LLVMModuleRef
    val namedValues: MutableMap<String, LLVMValueRef> = mutableMapOf()

//    val defaultTarget: BytePointer

    init {
//        LLVMInitializeAllTargetInfos()
//        LLVMInitializeAllTargets()
//        LLVMInitializeAllTargetMCs()
        LLVMLinkInMCJIT()
        LLVMInitializeNativeAsmPrinter()
        LLVMInitializeNativeAsmParser()
        LLVMInitializeNativeDisassembler()
        LLVMInitializeNativeTarget()
        llvmModule = LLVMModuleCreateWithName(module.name)
//        defaultTarget = LLVMGetDefaultTargetTriple()
//        LLVMSetTarget(llvmModule, defaultTarget)
//        LLVMCreateTargetMachine(LLVMTargetRef(defaultTarget), "", "generic", "", 0, 0, 0)

        module.globalFunctions.forEach { it.accept(this) }
    }

    override fun output(file: File) {
        val error = BytePointer(null as Pointer?) // Used to retrieve messages from functions
        LLVMVerifyModule(llvmModule, LLVMAbortProcessAction, error)
        LLVMDisposeMessage(error)

        val engine = LLVMExecutionEngineRef()
        if (LLVMCreateJITCompilerForModule(engine, llvmModule, 0, error) !== 0) {
            System.err.println(error.string)
            LLVMDisposeMessage(error)
            System.exit(-1)
        }

        val pass = LLVMCreatePassManager()
        LLVMAddConstantPropagationPass(pass)
        LLVMAddInstructionCombiningPass(pass)
        LLVMAddPromoteMemoryToRegisterPass(pass)
        LLVMAddGVNPass(pass)
        LLVMAddCFGSimplificationPass(pass)
        LLVMRunPassManager(pass, llvmModule)
        LLVMDumpModule(llvmModule)

        LLVMDisposePassManager(pass)
        LLVMDisposeExecutionEngine(engine)
    }

    override fun visit(function: Function) {
        // Get LLVM Types of Arguments
        val argument_types: List<LLVMTypeRef> = function.formals.map { getLLVMType(it.type)!! }
        // Get the FunctionType of the Function
        val llvmFunctionType: LLVMTypeRef = LLVMFunctionType(getLLVMType(function.returnType),
                PointerPointer<LLVMTypeRef>(*argument_types.toTypedArray()), argument_types.size, 0)
        // Add the Function to the Module
        val llvmFunction: LLVMValueRef = LLVMAddFunction(llvmModule, function.name, llvmFunctionType)
        namedValues.put(function.name, llvmFunction)

        LLVMSetFunctionCallConv(llvmFunction, LLVMCCallConv)
        val builder = LLVMCreateBuilder()

        val entryBlock = LLVMAppendBasicBlock(llvmFunction, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)

        val localVariables: MutableMap<String, LLVMValueRef> = mutableMapOf()

        var formalIndex = 0
        function.formals.forEach { localVariables.put(it.name, LLVMGetParam(llvmFunction, formalIndex)); formalIndex++ }

        var i = 0
        function.statements.forEach {
            visit(it, function, builder, llvmFunction, localVariables)
            localVariables.forEach { println("$i: ${it.key} -> ${it.value}") }
            i++
        }

        if (function.expression != null) {
            val ret_value = visit(function.expression, builder, localVariables)
            LLVMBuildRet(builder, ret_value)
        }

        LLVMDisposeBuilder(builder)
    }

    fun visit(statement: Statement, function: Function, builder: LLVMBuilderRef, llvmFunction: LLVMValueRef, localVariables: MutableMap<String, LLVMValueRef>) {
        when (statement) {
            is VariableDeclarationStatement -> {
                val variable = statement.variable
                if (variable.initialExpression != null) {
                    if (variable.constant) {
                        localVariables.put(variable.name, visit(variable.initialExpression, builder, localVariables))
                    } else {
                        val alloca = LLVMBuildAlloca(builder, getLLVMType(variable.type), variable.name)
                        val value = visit(variable.initialExpression, builder, localVariables)
                        val store = LLVMBuildStore(builder, value, alloca)
                        localVariables.put(variable.name, store)
                    }
                }
            }
        }
    }

    fun visit(expression: Expression, builder: LLVMBuilderRef, localVariables: MutableMap<String, LLVMValueRef>): LLVMValueRef {
        return when (expression) {
            is IntegerLiteral -> LLVMConstInt(LLVMInt32Type(), expression.value.toLong(), 0)
            is BinaryOperator -> {
                val A = visit(expression.expA, builder, localVariables)
                val B = visit(expression.expB, builder, localVariables)
                when (expression.operator) {
                    Operator.PLUS -> {
                        LLVMBuildAdd(builder, A, B, expression.toString())
                    }
                    Operator.MINUS -> {
                        LLVMBuildSub(builder, A, B, expression.toString())
                    }
                    Operator.MULTIPLY -> {
                        LLVMBuildMul(builder, A, B, expression.toString())
                    }
                    Operator.DIVIDE -> {
                        LLVMBuildSDiv(builder, A, B, expression.toString())
                    }
                    else -> LLVMConstInt(LLVMInt32Type(), 0, 0)
                }
            }
            is ReferenceExpression -> {
                val ref = expression.reference.name
                if (localVariables.containsKey(ref)) localVariables[ref]!!
                else LLVMConstInt(LLVMInt32Type(), 0, 0)
            }
            else -> LLVMConstInt(LLVMInt32Type(), 0, 0)
        }
    }

    companion object {

        fun getLLVMType(type: Type): LLVMTypeRef? {
            return when (type) {
                T_BOOL -> LLVMInt1Type()
                T_INT -> LLVMInt32Type()
                T_VOID -> LLVMVoidType()
                else -> null
            }
        }

    }

}
