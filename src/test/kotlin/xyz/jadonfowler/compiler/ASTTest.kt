package xyz.jadonfowler.compiler

import org.junit.Test
import xyz.jadonfowler.compiler.ast.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ASTTest {

    @Test fun functionDeclarationParsing() {
        val code = """
        testFunction0(argument0 : Int, argument1 : String, argument2 : Bool) : Int
            0
        """
        val module = compileString("functionDeclarationParsing", code)
        assertEquals("functionDeclarationParsing", module.name, "The Module's name is incorrect.")
        assertEquals(1, module.globalFunctions.size, "One Function should exist in the Module.")

        val testFunction0 = module.globalFunctions[0]
        assertEquals("testFunction0", testFunction0.name, "The name of the first Function isn't testFunction0.")

        assertEquals(0, testFunction0.statements.size, "testFunction0 shouldn't have any statements, but it has" +
                "${testFunction0.statements.size}.")
        assertNotNull(testFunction0.expression, "testFunction0 should have a return expression of 0.")

        assertEquals(3, testFunction0.formals.size, "testFunction0 has ${testFunction0.formals.size} formals instead" +
                " of 3.")

        assertEquals(T_BOOL, testFunction0.formals[2].type, "The third formal of testFunction0 should have a type of" +
                " Bool.")
    }

    @Test fun functionVariableDeclarationStatementParsing() {
        val code = """
        testFunction1(argument0 : Int) : Int
            let local_variable0 = argument1 * 32,
            local_variable0 - 8
        """
        val module = compileString("functionVariableDeclarationStatementParsing", code)
        val testFunction1 = module.globalFunctions[0]
        assertEquals(1, testFunction1.statements.size, "testFunction1 should only have 1 statement.")
        assertTrue(testFunction1.statements[0] is VariableDeclarationStatement, "The first statement should be a" +
                " VariableDeclarationStatement.")
        val statement = testFunction1.statements[0] as VariableDeclarationStatement
        assertEquals("local_variable0", statement.variable.name, "The variable's name in the statement isn't" +
                " local_variable0.")
        assertTrue(statement.variable.constant, "The variable is not a constant.")
        assertTrue(statement.variable.initialExpression is BinaryOperator, "The variable's initial expression is not" +
                " a Binary Operator.")
        val op = statement.variable.initialExpression as BinaryOperator
        assertTrue(op.expA is ReferenceExpression, "The variable's initial expression's left argument isn't a" +
                " ReferenceExpression.")
        assertTrue(op.expB is IntegerLiteral, "The variable's initial expression's right argument is not an" +
                " IntegerLiteral.")
    }
}
