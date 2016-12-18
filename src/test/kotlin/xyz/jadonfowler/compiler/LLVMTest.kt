package xyz.jadonfowler.compiler

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class LLVMTest {

    fun testIR(vararg testName: String) {
        val code = testName.map { "test/$it.l" }
        val files = code.map(::File)
        files.forEach { assertTrue(it.exists()) }

        val expectedIRFiles = testName.map { File("test/out/llvm/$it.ll") }
        expectedIRFiles.forEach { assertTrue(it.exists(), "Output file doesn't exist!") }
        val expectedIR = expectedIRFiles.map { it.readLines().joinToString("\n") }

        main(arrayOf("--llvm", "--ast", *code.toTypedArray()))

        val actualIR = testName.map { File("bin/$it.ll").readLines().joinToString("\n") }
        actualIR.forEach(::println)

        // endsWith is used to ignore header information, which is different on every platform
        actualIR.forEachIndexed { i, s -> assertTrue(s.endsWith(expectedIR[i]), "Wrong IR emitted!\n\nExpected:\n\n${expectedIR[i]}" +
                "\n\n(Note: Header information is ignored.)\n") }
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

    @Test fun genImportedDeclarations() {
        testIR("genImportedDeclarations")
    }

    @Test fun genRecursiveImport() {
        testIR("genRecursiveInput1", "genRecursiveInput2")
    }

    @Test fun genExternalFunction() {
        testIR("genExternalFunction")
    }

    @Test fun genImportExternalFunction() {
        testIR("genExternalFunction", "genImportExternalFunction")
    }

}
