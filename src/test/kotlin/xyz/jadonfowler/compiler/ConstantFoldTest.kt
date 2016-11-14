package xyz.jadonfowler.compiler

import org.junit.Test
import xyz.jadonfowler.compiler.ast.IntegerLiteral
import xyz.jadonfowler.compiler.pass.ConstantFoldPass
import xyz.jadonfowler.compiler.pass.PrintPass
import xyz.jadonfowler.compiler.pass.TypePass
import kotlin.test.assertEquals

class ConstantFoldTest {

    @Test fun additionConstantFolding() {
        val code = """
        let a = 10 + 24
        """
        val module = compileString("additionConstantFolding", code)
        TypePass(module)
        ConstantFoldPass(module)

        assertEquals(34, (module.globalVariables[0].initialExpression as IntegerLiteral).value)

        println(PrintPass(module).output)
    }

    @Test fun complexConstantFolding() {
        val code = """
        let a = 1234 * 12 + 8767 + 3453 - 57347 + 73457 + 7457
        """
        val module = compileString("complexConstantFolding", code)
        TypePass(module)
        ConstantFoldPass(module)

        assertEquals(50595, (module.globalVariables[0].initialExpression as IntegerLiteral).value)

        println(PrintPass(module).output)
    }

}
