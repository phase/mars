package xyz.jadonfowler.compiler.pass

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function

class ConstantFoldPass(module: Module) : Pass(module) {

    fun foldIntegerLiterals(originalExpression: Expression, a: IntegerLiteral, b: IntegerLiteral, operator: Operator): Expression {
        return when (operator) {
            Operator.PLUS_INT -> IntegerLiteral(a.value + b.value)
            Operator.MINUS_INT -> IntegerLiteral(a.value - b.value)
            Operator.MULTIPLY_INT -> IntegerLiteral(a.value * b.value)
            Operator.DIVIDE_INT -> IntegerLiteral(a.value / b.value)
            else -> originalExpression
        }
    }

    fun foldExpression(expression: Expression): Expression {
        return when (expression) {
            is BinaryOperator -> {
                val a = foldExpression(expression.expA)
                expression.expA = a
                val b = foldExpression(expression.expB)
                expression.expB = b
                var newExpression = expression
                if (a is IntegerLiteral && b is IntegerLiteral)
                    newExpression = foldIntegerLiterals(expression, a, b, expression.operator)
                else if (a is BinaryOperator && b is IntegerLiteral) {
                    val aa = a.expA
                    val ab = a.expB
                    if (a.operator == expression.operator) {
                        if (aa is IntegerLiteral)
                            newExpression = BinaryOperator(foldIntegerLiterals(a, aa, b, expression.operator), a.operator, ab)
                        else if (ab is IntegerLiteral)
                            newExpression = BinaryOperator(aa, a.operator, foldIntegerLiterals(a, ab, b, expression.operator))
                    }
                } else if (a is IntegerLiteral && b is BinaryOperator) {
                    val ba = b.expA
                    val bb = b.expB
                    if (b.operator == expression.operator) {
                        if (ba is IntegerLiteral)
                            newExpression = BinaryOperator(foldIntegerLiterals(b, ba, a, expression.operator), b.operator, bb)
                        else if (bb is IntegerLiteral)
                            newExpression = BinaryOperator(ba, b.operator, foldIntegerLiterals(b, bb, a, expression.operator))
                    }
                }
                newExpression
            }
            else -> expression
        }
    }

    init {
        module.globalVariables.forEach { it.accept(this) }
        module.globalFunctions.forEach { it.accept(this) }
        module.globalClasses.forEach { it.accept(this) }
    }

    override fun visit(clazz: Clazz) {
        clazz.fields.forEach { it.accept(this) }
        clazz.methods.forEach { it.accept(this) }
    }

    override fun visit(variable: Variable) {
        if (variable.initialExpression != null)
            variable.initialExpression = foldExpression(variable.initialExpression!!)
    }

    override fun visit(statement: Statement) {
        when (statement) {
            is VariableDeclarationStatement -> statement.variable.accept(this)
            is VariableReassignmentStatement -> statement.exp = foldExpression(statement.exp)
            is IfStatement -> {
                statement.exp = foldExpression(statement.exp)
                statement.statements.forEach { visit(it) }
                statement.elseStatement?.accept(this)
            }
            is WhileStatement -> {
                statement.exp = foldExpression(statement.exp)
                statement.statements.forEach { visit(it) }
            }
        }
    }

    override fun visit(function: Function) {
        function.statements.forEach { visit(it) }

        if (function.expression != null)
            function.expression = foldExpression(function.expression!!)
    }

}