package xyz.jadonfowler.compiler.visitor

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function

abstract class Visitor(val module: Module) {
    // Module Structure

    abstract fun visit(function: Function)
    abstract fun visit(formal: Formal)
    abstract fun visit(variable: Variable)
    abstract fun visit(clazz: Clazz)

    // Statements
    fun visit(statement: Statement) {
        when (statement) {
            is Block -> visit(statement)
            is IfStatement -> visit(statement)
            is WhileStatement -> visit(statement)
            is VariableDeclarationStatement -> visit(statement)
            is FunctionCallStatement -> visit(statement)
        }
    }

    abstract fun visit(block: Block)
    abstract fun visit(ifStatement: IfStatement)
    abstract fun visit(whileStatement: WhileStatement)
    abstract fun visit(variableDeclarationStatement: VariableDeclarationStatement)
    abstract fun visit(functionCallStatement: FunctionCallStatement)

    // Expressions
    fun visit(expression: Expression) {
        when (expression) {
            is TrueExpression -> visit(expression)
            is FalseExpression -> visit(expression)
            is IntegerLiteral -> visit(expression)
            is StringLiteral -> visit(expression)
            is ReferenceExpression -> visit(expression)
            is FunctionCallExpression -> visit(expression)
            is BinaryOperator -> visit(expression)
        }
    }

    abstract fun visit(trueExpression: TrueExpression)
    abstract fun visit(falseExpression: FalseExpression)
    abstract fun visit(integerLiteral: IntegerLiteral)
    abstract fun visit(stringLiteral: StringLiteral)
    abstract fun visit(referenceExpression: ReferenceExpression)
    abstract fun visit(functionCallExpression: FunctionCallExpression)
    abstract fun visit(binaryOperator: BinaryOperator)
}