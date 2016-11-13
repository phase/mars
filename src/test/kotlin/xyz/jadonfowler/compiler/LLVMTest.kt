package xyz.jadonfowler.compiler

import org.junit.Test
import xyz.jadonfowler.compiler.backend.LLVMBackend
import xyz.jadonfowler.compiler.pass.PrintPass
import xyz.jadonfowler.compiler.pass.TypePass
import java.io.File

class LLVMTest {

    @Test fun genGlobalConstant() {
        val code = """
        let a = 7
        """
        val module = compileString("genGlobalConstant", code)

        TypePass(module)
        println(PrintPass(module).output)
        LLVMBackend(module).output(File("/dev/null"))
    }

    @Test fun genFunction() {
        val code = """
        test (a : Int, b : Int, c : Int)
            0
        """
        val module = compileString("genFunction", code)

        TypePass(module)
        println(PrintPass(module).output)
        LLVMBackend(module).output(File("/dev/null"))
    }

    @Test fun genVariableDeclaration() {
        val code = """
        test ()
            let a = 5,
            0
        """
        val module = compileString("genVariableDeclaration", code)

        TypePass(module)
        println(PrintPass(module).output)
        LLVMBackend(module).output(File("/dev/null"))
    }

    @Test fun genComplexExpressions() {
        val code = """
        test (z : Int, y : Int, x : Int, w : Int)
            var v = 42 + x,
            let u = 45 + v * 67 + 124 - (w * 4) / 5,
            let d = v * 2 - z,
            5 + u * z * v + d
        """
        val module = compileString("genComplexExpressions", code)

        TypePass(module)
        println(PrintPass(module).output)
        LLVMBackend(module).output(File("/dev/null"))
    }

    @Test fun genVariableReassignment() {
        val code = """
        test ()
            var a = 0,
            a = 1,
            a = 2,
            a = 3,
            a
        """
        val module = compileString("genVariableReassignment", code)

        TypePass(module)
        println(PrintPass(module).output)
        LLVMBackend(module).output(File("/dev/null"))
    }

    @Test fun genIfStatement() {
        val code = """
        testBranching (a : Int, b : Int, c : Int)
            var r = 0,
            if a == 10
                r = b
            else
                r = c
            ;,
            r
        """
        val module = compileString("genIfStatement", code)

        TypePass(module)
        println(PrintPass(module).output)
        LLVMBackend(module).output(File("/dev/null"))
    }

    @Test fun genGlobalsInFunctions() {
        val code = """
        let G0 = 1234 + 4321
        let G1 = 1 + 2 - 3 * 4 + 6 / 6
        let G2 = 4

        test (z : Int, y : Int, x : Int, w : Int)
            var v = 42 + x,
            let u = 45 + v * 67 + 124 - (w * 4) / 5,
            v = v * 2 - z,
            var t = 1,
            if z < 10
                t = v * z
            else
                t = v - z
            ;
            let l = 74 * 3 - v + z * x - w,
            5 + u * z * v + t * G2 - G0 * G1 + 2 * l

        returnGlobal ()
            G0
        """
        val module = compileString("genGlobalsInFunctions", code)

        TypePass(module)
        println(PrintPass(module).output)
        LLVMBackend(module).output(File("/dev/null"))
    }

    @Test fun genOperators() {
        val code = """
        test (a : Int, b : Int) : Int
            var r = 0,
            let c : Bool = a != b,
            if c
                r = r + 10
            else
                if a > b
                    r = r + 11
                else
                    if a <= b
                        r = r + 12
                    ;
                ;
            ;
            let g = 21,
            g + r
        """
        val module = compileString("genOperators", code)

        TypePass(module)
        println(PrintPass(module).output)
        LLVMBackend(module).output(File("/dev/null"))
    }

}
