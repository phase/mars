package xyz.jadonfowler.compiler.ast.pass

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import xyz.jadonfowler.compiler.ast.visitor.Visitor

class TypePass : Visitor() {

    fun getType(expression: Expression): Type {
        return when (expression) {
            is IntegerLiteral -> T_INT
            is BinaryOperator -> {
                val typeA = getType(expression.expA)
                val typeB = getType(expression.expB)
                if (typeA != typeB)
                    throw Exception("${typeA.toString()} is not the same type as ${typeB.toString()} for expression ${expression.toString()}.")
                typeA
            }
            is TrueExpression, is FalseExpression -> {
                T_BOOL
            }
            else -> T_UNDEF
        }
    }

    override fun visit(module: Module) {
        module.globalVariables.forEach { visit(it) }
        module.globalFunctions.forEach { visit(it) }
    }

    override fun visit(function: Function) {
    }

    override fun visit(formal: Formal) {
    }

    override fun visit(variable: Variable) {
        if (variable.initialExpression != null) {
            val expressionType = getType(variable.initialExpression)
            if (!variable.type.equals(T_UNDEF) && !variable.type.equals(expressionType))
                throw Exception("Variable '${variable.name}' is marked with the type '${variable.type.toString()}' but its initial expression is of type '${expressionType.toString()}'.")
            variable.type = expressionType
            println("[TypePass] Variable '${variable.name}' now has the type '${variable.type.toString()}'")
        }
    }

    override fun visit(clazz: Clazz) {
    }

    override fun visit(block: Block) {
    }

    override fun visit(ifStatement: IfStatement) {
    }

    override fun visit(whileStatement: WhileStatement) {
    }

    override fun visit(variableDeclarationStatement: VariableDeclarationStatement) {
        if (variableDeclarationStatement.variable.type.equals(T_UNDEF)) {
            variableDeclarationStatement.variable.type = getType(variableDeclarationStatement.variable.initialExpression!!)
        }
    }

    override fun visit(functionCallStatement: FunctionCallStatement) {
    }

    override fun visit(trueExpression: TrueExpression) {
    }

    override fun visit(falseExpression: FalseExpression) {
    }

    override fun visit(integerLiteral: IntegerLiteral) {
    }

    override fun visit(identifierExpression: IdentifierExpression) {
    }

    override fun visit(binaryOperator: BinaryOperator) {
    }
}