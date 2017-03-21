package xyz.jadonfowler.compiler

import org.junit.Test
import xyz.jadonfowler.compiler.pass.SemanticAnalysis
import kotlin.test.assertTrue

class SemanticsTest {

    @Test fun mutableGlobalVariable() {
        val code = """
        var a = 7
        var b = 8
        let c = 9
        """
        val module = compileString("mutableGlobalVariable", code)

        SemanticAnalysis(module)
        assertTrue(module.errors.size > 0)
        module.errors.forEach(::println)
    }

}