package xyz.jadonfowler.compiler.backend

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import java.io.File

class LLVMBackend(module: Module) : Backend(module) {

    val llvmModule: LLVMModuleRef

    init {
        LLVMLinkInMCJIT()
        LLVMInitializeNativeAsmPrinter()
        LLVMInitializeNativeAsmParser()
        LLVMInitializeNativeDisassembler()
        LLVMInitializeNativeTarget()
        llvmModule = LLVMModuleCreateWithName(module.name)

        module.globalFunctions.forEach { it.accept(this) }
    }

    override fun output(file: File) {
        val error = BytePointer(null as Pointer?) // Used to retrieve messages from functions
        LLVMVerifyModule(llvmModule, LLVMAbortProcessAction, error)
        LLVMDisposeMessage(error)

        val engine = LLVMExecutionEngineRef()
        if (LLVMCreateJITCompilerForModule(engine, llvmModule, 2, error) !== 0) {
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
        // LLVMDisposeBuilder(builder)
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

        LLVMSetFunctionCallConv(llvmFunction, LLVMCCallConv)
        val builder = LLVMCreateBuilder()

        val entryBlock = LLVMAppendBasicBlock(llvmFunction, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)

        function.statements.forEach { it.accept(this) }

        val ret_value = LLVMConstInt(LLVMInt32Type(), 0, 0)
        LLVMBuildRet(builder, ret_value)

        LLVMDisposeBuilder(builder)
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
