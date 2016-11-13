package xyz.jadonfowler.compiler.pass

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function

class TypePass(module: Module) : Pass(module) {

    var currentFunction: Function? = null

    /**
     * Get an Expression's Type
     */
    fun getType(expression: Expression, localVariables: MutableMap<String, Variable>?): Type {
        return when (expression) {
            is IntegerLiteral -> T_INT
            is BinaryOperator -> {
                // Expressions A & B should have the same type.
                val typeA = getType(expression.expA, localVariables)
                val typeB = getType(expression.expB, localVariables)
                if (typeA != typeB)
                    throw Exception("$typeA is not the same type as $typeB for expression $expression.")
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
                val function = module.globalFunctions.filter { it.name == functionName }.first()
                function.returnType
            }
            is ReferenceExpression -> {
                val name = expression.reference.name
                val thingsWithName: MutableList<Node> = mutableListOf()

                thingsWithName.addAll(module.globalClasses.filter { it.name == name })
                thingsWithName.addAll(module.globalFunctions.filter { it.name == name })
                thingsWithName.addAll(module.globalVariables.filter { it.name == name })
                if (currentFunction != null) thingsWithName.addAll(currentFunction?.formals!!)
                localVariables?.forEach { if (it.key == name) thingsWithName.add(it.value) }

                if (thingsWithName.isNotEmpty()) {
                    val lastThingWithName = thingsWithName.last()
                    when (lastThingWithName) {
                        is Clazz -> lastThingWithName
                        is Function -> lastThingWithName
                        is Variable -> lastThingWithName.type
                        is Formal -> lastThingWithName.type
                        else -> T_UNDEF
                    }
                } else
                    T_UNDEF
            }
            else -> T_UNDEF
        }
    }

    init {
        module.globalVariables.forEach { it.accept(this) }
        module.globalFunctions.forEach { it.accept(this) }
        module.globalClasses.forEach { it.accept(this) }
    }

    override fun visit(function: Function) {
        currentFunction = function
        val localVariables: MutableMap<String, Variable> = mutableMapOf()
        function.statements.forEach { visit(it, localVariables) }
        if (function.expression != null) {
            // Function should return the type of the return expression.
            val lastExpressionType = getType(function.expression, localVariables)
            if (function.returnType != lastExpressionType && function.returnType != T_UNDEF)
                throw Exception("Function '${function.name}' is marked with the type " +
                        "'${function.returnType}' but its last expression is of the type '$lastExpressionType'.")
            function.returnType = lastExpressionType
        } else {
            // Function should return void if there is no return expression.
            if (function.returnType != T_UNDEF && function.returnType != T_VOID)
                throw Exception("Function '${function.name}' is marked with the type " +
                        "'${function.returnType}' but does not contain a final expression that returns " +
                        "that type.")
            function.returnType = T_VOID
        }
        currentFunction = null
    }

    fun visit(variable: Variable, localVariables: MutableMap<String, Variable>?) {
        if (variable.initialExpression != null) {
            // Variable should have the same type as their initial expression.
            val expressionType = getType(variable.initialExpression, localVariables)
            if (variable.type != T_UNDEF && variable.type != expressionType)
                throw Exception("Variable '${variable.name}' is marked with the type '${variable.type}' " +
                        "but its initial expression is of type '$expressionType'.")
            variable.type = expressionType
        }
    }

    override fun visit(variable: Variable) {
        visit(variable, null)
    }

    override fun visit(clazz: Clazz) {
        clazz.fields.forEach { it.accept(this) }
        clazz.methods.forEach { it.accept(this) }
    }

    fun visit(statement: Statement, localVariables: MutableMap<String, Variable>?) {
        when (statement) {
            is VariableDeclarationStatement -> visit(statement, localVariables)
            is VariableReassignmentStatement -> visit(statement, localVariables)
            else -> visit(statement)
        }
    }

    fun visit(variableDeclarationStatement: VariableDeclarationStatement, localVariables: MutableMap<String, Variable>?) {
        localVariables?.put(variableDeclarationStatement.variable.name, variableDeclarationStatement.variable)
        visit(variableDeclarationStatement.variable, localVariables)
    }

    fun visit(variableReassignmentStatement: VariableReassignmentStatement, localVariables: MutableMap<String, Variable>?) {
        val name = variableReassignmentStatement.reference.name
        val thingsWithName: MutableList<Variable> = mutableListOf()
        thingsWithName.addAll(module.globalVariables.filter { it.name == name })
        if (currentFunction != null) thingsWithName.addAll(currentFunction?.formals!!)
        localVariables?.forEach { if (it.key == name) thingsWithName.add(it.value) }

        if(thingsWithName.isNotEmpty()) {
            val variable = thingsWithName.last()
            val expressionType = getType(variableReassignmentStatement.exp, localVariables)
            if(variable.type != expressionType)
                throw Exception("Variable '${variable.name}' is marked with the type '${variable.type}' but the type" +
                        " of '${variableReassignmentStatement.exp} is '$expressionType'.")
        }
    }

}