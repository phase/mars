package xyz.jadonfowler.compiler

import org.junit.Test
import xyz.jadonfowler.compiler.pass.SemanticAnalysis
import xyz.jadonfowler.compiler.pass.TypePass
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

    @Test fun referenceUsing() {
        val code = """
        f () : Int
            let a = 7,
            let b = a,
            let c = a + b,
            0
        """
        val module = compileString("referenceUsing", code)

        SemanticAnalysis(module)
        assertTrue(module.errors.size > 0)
        module.errors.forEach(::println)
    }

    @Test fun referenceUsing2() {
        val code = """
        class P
            let x : Int

            init (v : Int)
                x = v
        ;

        main () : Int
            let a = new P(7),
            let b = a,
            let c = a.x + b.x,
            0
        """
        val module = compileString("referenceUsing2", code)

        TypePass(module)
        SemanticAnalysis(module)
        assertTrue(module.errors.size > 0)
        module.errors.forEach(::println)
    }

}
