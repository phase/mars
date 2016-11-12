package xyz.jadonfowler.compiler.pass

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import xyz.jadonfowler.compiler.visitor.Visitor

class TypePass(module: Module) : Visitor(module) {

    var currentFunction: Function? = null

    /**
     * Get an Expression's Type
     */
    fun getType(expression: Expression): Type {
        return when (expression) {
            is IntegerLiteral -> T_INT
            is BinaryOperator -> {
                // Expressions A & B should have the same type.
                val typeA = getType(expression.expA)
                val typeB = getType(expression.expB)
                if (typeA != typeB)
                    throw Exception("${typeA.toString()} is not the same type as ${typeB.toString()} for " +
                            "expression ${expression.toString()}.")
                typeA
            }
            is TrueExpression, is FalseExpression -> {
                T_BOOL
            }
            is StringLiteral -> {
                T_STRING
            }
            is FunctionCallExpression -> {
                val functionName = expression.functionReference.name
                val function = module.globalFunctions.filter { it.name.equals(functionName) }.first()
                function.returnType
            }
            is ReferenceExpression -> {
                val name = expression.reference.name
                val thingsWithName: MutableList<Node> = mutableListOf()
                thingsWithName.addAll(module.globalClasses.filter { it.name.equals(name) })
                thingsWithName.addAll(module.globalFunctions.filter { it.name.equals(name) })
                thingsWithName.addAll(module.globalVariables.filter { it.name.equals(name) })
                if (currentFunction != null) {
                    thingsWithName.addAll(currentFunction?.formals!!)
                }
                if (thingsWithName.isNotEmpty()) {
                    val firstThingWithName = thingsWithName.first()
                    when (firstThingWithName) {
                        is Clazz -> firstThingWithName
                        is Function -> firstThingWithName
                        is Variable -> firstThingWithName.type
                        is Formal -> firstThingWithName.type
                        else -> T_UNDEF
                    }
                } else
                    T_UNDEF
            }
            else -> T_UNDEF
        }
    }

    init {
        module.globalFunctions.forEach { it.accept(this) }
        module.globalVariables.forEach { it.accept(this) }
        module.globalClasses.forEach { it.accept(this) }
    }

    override fun visit(function: Function) {
        currentFunction = function
        function.statements.forEach { it.accept(this) }
        if (function.expression != null) {
            // Function should return the type of the return expression.
            val lastExpressionType = getType(function.expression)
            if (function.returnType != lastExpressionType && !function.returnType.equals(T_UNDEF))
                throw Exception("Function '${function.name}' is marked with the type " +
                        "'${function.returnType.toString()}' but its last expression is of the type " +
                        "'${lastExpressionType.toString()}'.")
            function.returnType = lastExpressionType
        } else {
            // Function should return void if there is no return expression.
            if (!function.returnType.equals(T_UNDEF) && !function.returnType.equals(T_VOID))
                throw Exception("Function '${function.name}' is marked with the type " +
                        "'${function.returnType.toString()}' but does not contain a final expression that returns " +
                        "that type.")
            function.returnType = T_VOID
        }
        currentFunction = null
    }

    override fun visit(formal: Formal) {

    }

    override fun visit(variable: Variable) {
        if (variable.initialExpression != null) {
            // Variable should have the same type as their initial expression.
            val expressionType = getType(variable.initialExpression)
            if (!variable.type.equals(T_UNDEF) && !variable.type.equals(expressionType))
                throw Exception("Variable '${variable.name}' is marked with the type '${variable.type.toString()}' " +
                        "but its initial expression is of type '${expressionType.toString()}'.")
            variable.type = expressionType
        }
    }

    override fun visit(clazz: Clazz) {
        clazz.fields.forEach { it.accept(this) }
        clazz.methods.forEach { it.accept(this) }
    }

    override fun visit(block: Block) {
    }

    override fun visit(ifStatement: IfStatement) {
    }

    override fun visit(whileStatement: WhileStatement) {
    }

    override fun visit(variableDeclarationStatement: VariableDeclarationStatement) {
        variableDeclarationStatement.variable.accept(this)
    }

    override fun visit(functionCallStatement: FunctionCallStatement) {
    }

    override fun visit(trueExpression: TrueExpression) {
    }

    override fun visit(falseExpression: FalseExpression) {
    }

    override fun visit(integerLiteral: IntegerLiteral) {
    }

    override fun visit(stringLiteral: StringLiteral) {
    }

    override fun visit(referenceExpression: ReferenceExpression) {
    }

    override fun visit(functionCallExpression: FunctionCallExpression) {
    }

    override fun visit(binaryOperator: BinaryOperator) {
    }
}