package xyz.jadonfowler.compiler

import org.junit.Test
import xyz.jadonfowler.compiler.backend.LLVMBackend
import xyz.jadonfowler.compiler.pass.ConstantFoldPass
import xyz.jadonfowler.compiler.pass.PrintPass
import xyz.jadonfowler.compiler.pass.TypePass
import java.io.File
import kotlin.test.assertTrue

class LLVMTest {

    fun testIR(testName: String) {
        val code = "examples/test/$testName.l"
        val file = File(code)
        assertTrue(file.exists())

        val expectedIRFile = File("examples/test/out/$testName.ll")
        assertTrue(expectedIRFile.exists())
        val expectedIR = expectedIRFile.readLines().joinToString("\n")

        main(arrayOf(code, "--llvm"))

        val actualIR = File("bin/$testName.ll").readLines().joinToString("\n")
        // endsWith is used to ignore header information, which is different on every platform
        assertTrue(actualIR.endsWith(expectedIR), "Wrong IR emitted! \n\nExpected:\n\n$expectedIR\n\nActual:\n" +
                "\n$actualIR\n\n(Note: Header information is ignored.)\n")
    }

    @Test fun genGlobalConstant() {
        testIR("genGlobalConstant")
    }

    @Test fun genFunction() {
        testIR("genFunction")
    }

    @Test fun genVariableDeclaration() {
        testIR("genVariableDeclaration")
    }

    @Test fun genComplexExpressions() {
        testIR("genComplexExpressions")
    }

    @Test fun genVariableReassignment() {
        testIR("genVariableReassignment")
    }

    @Test fun genIfStatement() {
        testIR("genIfStatement")
    }

    @Test fun genGlobalsInFunctions() {
        testIR("genGlobalsInFunctions")
    }

    @Test fun genOperators() {
        testIR("genOperators")
    }

    @Test fun genWhileLoop() {
        testIR("genWhileLoop")
    }

    @Test fun genComplexExpressionsInWhileLoop() {
        testIR("genComplexExpressionsInWhileLoop")
    }

}
