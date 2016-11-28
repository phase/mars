package xyz.jadonfowler.compiler

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class LLVMTest {

    fun testIR(testName: String) {
        val code = "test/$testName.l"
        val file = File(code)
        assertTrue(file.exists())

        val expectedIRFile = File("test/out/llvm/$testName.ll")
        assertTrue(expectedIRFile.exists(), "Output file doesn't exist!")
        val expectedIR = expectedIRFile.readLines().joinToString("\n")

        main(arrayOf(code, "--llvm"))

        val actualIR = File("bin/$testName.ll").readLines().joinToString("\n")
        println(actualIR)

        // endsWith is used to ignore header information, which is different on every platform
        assertTrue(actualIR.endsWith(expectedIR), "Wrong IR emitted!\n\nExpected:\n\n$expectedIR" +
                "\n\n(Note: Header information is ignored.)\n")
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

    @Test fun genInfixFunctionCall() {
        testIR("genInfixFunctionCall")
    }

    @Test fun genRecursiveCall() {
        testIR("genRecursiveCall")
    }

}
