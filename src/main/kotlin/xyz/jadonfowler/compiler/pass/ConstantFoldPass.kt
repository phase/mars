package xyz.jadonfowler.compiler.pass

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function

class ConstantFoldPass(module: Module) : Pass(module) {

    fun foldExpression(expression: Expression): Expression {
        return when (expression) {
            is BinaryOperator -> {
                var newExpression = expression
                val a = foldExpression(expression.expA)
                val b = foldExpression(expression.expB)
                if (a is IntegerLiteral && b is IntegerLiteral) {
                    when (expression.operator) {
                        Operator.PLUS -> newExpression = IntegerLiteral(a.value + b.value)
                        Operator.MINUS -> newExpression = IntegerLiteral(a.value - b.value)
                        Operator.MULTIPLY -> newExpression = IntegerLiteral(a.value * b.value)
                        Operator.DIVIDE -> newExpression = IntegerLiteral(a.value / b.value)
                        else -> {
                        }
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

    override fun visit(function: Function) {
        function.statements.forEach {
            when (it) {
                is VariableDeclarationStatement -> it.variable.accept(this)
                is VariableReassignmentStatement -> it.exp = foldExpression(it.exp)
            }
        }

        if (function.expression != null)
            function.expression = foldExpression(function.expression!!)
    }

}