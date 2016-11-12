package xyz.jadonfowler.compiler.backend

import org.bytedeco.javacpp.*
import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import org.bytedeco.javacpp.LLVM.*
import java.io.File

class LLVMBackend(module: Module) : Backend(module) {

    val llvmModule: LLVMModuleRef
    var error = BytePointer(null as Pointer?) // Used to retrieve messages from functions

    init {
        LLVMLinkInMCJIT()
        LLVMInitializeNativeAsmPrinter()
        LLVMInitializeNativeAsmParser()
        LLVMInitializeNativeDisassembler()
        LLVMInitializeNativeTarget()
        llvmModule = LLVMModuleCreateWithName(module.name)
    }

    override fun output(file: File) {
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
    }

    override fun visit(formal: Formal) {
    }

    override fun visit(variable: Variable) {
    }

    override fun visit(clazz: Clazz) {
    }

    override fun visit(block: Block) {
    }

    override fun visit(ifStatement: IfStatement) {
    }

    override fun visit(whileStatement: WhileStatement) {
    }

    override fun visit(variableDeclarationStatement: VariableDeclarationStatement) {
    }

    override fun visit(functionCallStatement: FunctionCallStatement) {
    }

    override fun visit(trueExpression: TrueExpression) {
    }

    override fun visit(falseExpression: FalseExpression) {
    }

    override fun visit(integerLiteral: IntegerLiteral) {
    }

    override fun visit(stringLiteral: StringLiteral) {
    }

    override fun visit(referenceExpression: ReferenceExpression) {
    }

    override fun visit(functionCallExpression: FunctionCallExpression) {
    }

    override fun visit(binaryOperator: BinaryOperator) {
    }

}