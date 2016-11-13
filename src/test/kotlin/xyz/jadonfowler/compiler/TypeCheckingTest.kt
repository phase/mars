package xyz.jadonfowler.compiler

import org.junit.Test
import xyz.jadonfowler.compiler.ast.T_BOOL
import xyz.jadonfowler.compiler.ast.T_INT
import xyz.jadonfowler.compiler.ast.T_STRING
import xyz.jadonfowler.compiler.ast.VariableDeclarationStatement
import xyz.jadonfowler.compiler.pass.PrintPass
import xyz.jadonfowler.compiler.pass.TypePass
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TypeCheckingTest {

    @Test fun inferGlobalVariableTypes() {
        val code = """
        let a = 7
        """
        val module = compileString("inferGlobalVariableTypes", code)
        TypePass(module)

        assertEquals(T_INT, module.globalVariables[0].type)

        println(PrintPass(module).output)
    }

    @Test fun inferLocalVariableTypes() {
        val code = """
        test (a : Int, b : Int, c : Int) : Int
            let d = a + b,
            let e = "test",
            let f = true,
            7 + c
        """
        val module = compileString("inferLocalVariableTypes", code)
        TypePass(module)

        assertEquals(3, module.globalFunctions[0].statements.size)
        val statements = module.globalFunctions[0].statements
        assertEquals(T_INT, (statements[0] as VariableDeclarationStatement).variable.type)
        assertEquals(T_STRING, (statements[1] as VariableDeclarationStatement).variable.type)
        assertEquals(T_BOOL, (statements[2] as VariableDeclarationStatement).variable.type)

        println(PrintPass(module).output)
    }

    @Test fun inferFunctionReturnType() {
        val code = """
        test (a : Int, b : Int, c : Int)
            let d = a + b,
            let e = b * d - a + b,
            let f = e / b * d - a + b + c * c,
            7 + f
        """
        val module = compileString("inferFunctionReturnType", code)
        TypePass(module)

        assertEquals(T_INT, module.globalFunctions[0].returnType)

        println(PrintPass(module).output)
    }

    @Test fun incorrectTypeSigOnGlobalVariable() {
        val code = """
        let a : String = 3
        """
        val module = compileString("incorrectTypeSigOnGlobalVariable", code)

        assertFails { TypePass(module) }

        println(PrintPass(module).output)
    }

    @Test fun incorrectTypeSigOnLocalVariable() {
        val code = """
        test () : Int
            let a : Void = "test",
            6
        """
        val module = compileString("incorrectTypeSigOnLocalVariable", code)

        assertFails { TypePass(module) }

        println(PrintPass(module).output)
    }

    @Test fun incorrectReassignmentType() {
        val code = """
        test () : Int
            let a = 7,
            a = "test",
            6
        """
        val module = compileString("incorrectReassignmentType", code)

        assertFails { TypePass(module) }

        println(PrintPass(module).output)
    }
}
