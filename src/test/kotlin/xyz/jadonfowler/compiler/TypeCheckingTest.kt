package xyz.jadonfowler.compiler

import org.junit.Test
import xyz.jadonfowler.compiler.ast.*
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

    @Test fun correctTypeMarkingAndInference() {
        val code = """
        let d = 3 + 2 let e = 0 let f : Int
        let h = 6 let i : Int = 7 let j : Int = 8
        let str = "test"

        llvm (z : Int, y : Int, x : Int, w : Int)
            var v = 42 + x,
            let u = 45 + v * 67 + 124 - (w * 4) / 5,
            v = v * 2 - z,
            5 + u * z * v

        foo (t : Int, s : Int)
            let r : Int = 90128,
            if 1 != (2 + 2)
                var q = s,
                if 2 != 14 * 7 - 5
                    q = t
                else
                    if 4 >= 2
                        q = 7,
                        if s >= t || s <= 8:
                            print(t, s)
                        ;
                    ;
                ;
            ;
            thing(a + b, a - b * g),
            a + b + 1

        class Object

            let field : Int = 0

            method (arg : Int)
                let local : Int = arg + 7,
                let thing : Int = local * 5,
                local / thing
        ;

        let variable_defined_after_class : Int = 0
        """
        val module = compileString("correctTypeMarkingAndInference", code)
        TypePass(module)

        assertEquals(T_INT, module.globalClasses[0].methods[0].returnType)
        assertEquals(T_INT, module.globalFunctions[0].returnType)
        assertEquals(T_INT, ((module.globalFunctions[1].statements[1] as IfStatement).statements[0]
                as VariableDeclarationStatement).variable.type)

        println(PrintPass(module).output)
    }

    @Test fun incorrectConditionTypeInIfStatement() {
        val code = """
        test () : Int
            let a = 7,
            if a
                let b = 7
            ;
            6
        """
        val module = compileString("incorrectConditionTypeInIfStatement", code)

        assertFails { TypePass(module) }

        println(PrintPass(module).output)
    }

    @Test fun incorrectConditionTypeInWhileStatement() {
        val code = """
        test () : Int
            let a = 7,
            while a
                let b = 7
            ;
            6
        """
        val module = compileString("incorrectConditionTypeInWhileStatement", code)

        assertFails { TypePass(module) }

        println(PrintPass(module).output)
    }
}
