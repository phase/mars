package xyz.jadonfowler.compiler.pass

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function

class TypePass(module: Module) : Pass(module) {

    /**
     * Get an Expression's Type
     */
    fun getType(expression: Expression, localVariables: MutableMap<String, Variable>?): Type {
        return when (expression) {
            is IntegerLiteral -> T_INT32
            is FloatLiteral -> expression.type
            is BinaryOperator -> {
                val returnType = expression.operator.returnType

                val expA = expression.expA
                val expB = expression.expB

                val checkType = { expA: Expression, expB: Expression, expectedTypeOfA: Type, expectedTypeOfB: Type ->
                    if (expectedTypeOfA != T_UNDEF) {
                        if (expA is ReferenceExpression) {
                            val node = module.getNodeFromReference(expA.reference, localVariables)
                            when (node) {
                                is Variable -> {
                                    if (node.type == T_UNDEF)
                                        node.type = expectedTypeOfA
                                    else if (node.type != expectedTypeOfA && (node.type !is IntType && expectedTypeOfA !is IntType))
                                        module.errors.add("${node.name} has the type ${node.type} but was " +
                                                "expected to have the type $expectedTypeOfA.")
                                }
                            }
                        } else if (expA is FunctionCallExpression) {
                            val node = module.getNodeFromReference(expA.functionCall.functionReference, localVariables)
                            when (node) {
                                is Function -> {
                                    if (node.returnType == T_UNDEF)
                                        node.returnType = expectedTypeOfA
                                    else if (node.returnType != expectedTypeOfA && (node.returnType !is IntType && expectedTypeOfA !is IntType))
                                        module.errors.add("${node.name} has the return type ${node.returnType} but was" +
                                                "expected to have the type $expectedTypeOfA.")
                                }
                            }
                        }
                    } else if (expectedTypeOfB == T_UNDEF) {
                        if (expA is ReferenceExpression) {
                            val node = module.getNodeFromReference(expA.reference, localVariables)
                            when (node) {
                                is Variable -> {
                                    if (node.type == T_UNDEF) {
                                        node.type = getType(expB, localVariables)
                                    }
                                }
                            }
                        } else if (expA is FunctionCallExpression) {
                            val node = module.getNodeFromReference(expA.functionCall.functionReference, localVariables)
                            when (node) {
                                is Function -> {
                                    if (node.returnType == T_UNDEF)
                                        node.returnType = getType(expB, localVariables)
                                }
                            }
                        }
                    }
                }
                checkType(expA, expB, expression.operator.aType, expression.operator.bType)
                checkType(expB, expA, expression.operator.bType, expression.operator.aType)

                if (returnType is IntType) {
                    getBiggestInt(getType(expA, localVariables) as IntType, getType(expB, localVariables) as IntType)
                } else if(returnType is FloatType) {
                    getBiggestFloat(getType(expA, localVariables) as FloatType, getType(expB, localVariables) as FloatType)
                } else
                    returnType
            }
            is TrueExpression, is FalseExpression -> {
                T_BOOL
            }
            is StringLiteral -> {
                T_STRING
            }
            is FunctionCallExpression -> {
                val function = module.getNodeFromReference(expression.functionCall.functionReference, null)
                if (function is Function)
                    function.returnType
                else {
                    module.errors.add("Can't find return type for '$expression'.")
                    T_UNDEF
                }
            }
            is ClazzInitializerExpression -> {
                val node = module.getNodeFromReference(expression.classReference, null)
                node as? Clazz ?: T_UNDEF
            }
            is FieldGetterExpression -> {
                val varType = localVariables?.get(expression.variableReference.name)?.type
                if (varType is Clazz) {
                    val fieldName = expression.fieldReference.name
                    val possibleFields = varType.fields.filter { it.name == fieldName }
                    val field = possibleFields.last()
                    field.type
                } else T_UNDEF
            }
            is ReferenceExpression -> {
                val node = module.getNodeFromReference(expression.reference, localVariables)
                if (node != null) {
                    when (node) {
                        is Clazz -> node
                        is Function -> node
                        is Variable -> node.type
                        is Formal -> node.type
                        else -> T_UNDEF
                    }
                } else {
                    module.errors.add("Can't find type for '$expression'.")
                    T_UNDEF
                }
            }
            else -> T_UNDEF
        }
    }

    init {
        module.globalVariables.forEach { it.accept(this) }
        module.globalClasses.forEach { it.accept(this) }
        module.globalFunctions.forEach { it.accept(this) }
    }

    override fun visit(function: Function) {
        visit(function, null)
    }

    fun visit(function: Function, clazz: Clazz?) {
        val localVariables: MutableMap<String, Variable> = mutableMapOf()

        if (clazz != null) {
            println()
            clazz.fields.forEach { localVariables.put(it.name, it) }
        }

        /*
         * add types to untyped formals, such as
         *     add (a, b : Int) a + b
         * a's type is set to b's type
         */
        var previousFormalType = T_UNDEF
        function.formals.reversed().forEach {
            if (it.type == T_UNDEF)
                it.type = previousFormalType
            else
                previousFormalType = it.type
            localVariables.put(it.name, it)
        }

        function.statements.forEach { visit(it, localVariables) }
        if (function.expression != null) {
            // Function should return the type of the return expression.
            val lastExpressionType = getType(function.expression!!, localVariables)
            if (function.returnType != lastExpressionType && function.returnType != T_UNDEF)
                module.errors.add("Function '${function.name}' is marked with the type " +
                        "'${function.returnType}' but its last expression is of the type '$lastExpressionType'.")
            function.returnType = lastExpressionType
        } else {
            // Function should return void if there is no return expression.
            if (function.returnType != T_UNDEF && function.returnType != T_VOID)
                module.errors.add("Function '${function.name}' is marked with the type " +
                        "'${function.returnType}' but does not contain a final expression that returns " +
                        "that type.")
            function.returnType = T_VOID
        }
    }

    fun visit(variable: Variable, localVariables: MutableMap<String, Variable>?) {
        if (variable.initialExpression != null) {
            visit(variable.initialExpression!!, localVariables)
            // Variable should have the same type as their initial expression.
            val expressionType = getType(variable.initialExpression!!, localVariables)
            if (variable.type != T_UNDEF && variable.type != expressionType)
                module.errors.add("Variable '${variable.name}' is marked with the type '${variable.type}' " +
                        "but its initial expression is of type '$expressionType'.")
            variable.type = expressionType
        }
    }

    override fun visit(variable: Variable) {
        visit(variable, null)
    }

    override fun visit(clazz: Clazz) {
        clazz.fields.forEach { it.accept(this) }
        clazz.methods.forEach { visit(it, clazz) }
    }

    fun visit(expression: Expression, localVariables: MutableMap<String, Variable>?) {
        when (expression) {
            is FunctionCallExpression -> visit(expression, localVariables)
        }
    }

    fun visit(functionCallExpression: FunctionCallExpression, localVariables: MutableMap<String, Variable>?) {
        val function = module.globalFunctions.filter { it.name == functionCallExpression.functionCall.functionReference.name }.last()
        val formalTypes = function.formals.map { it.type }
        val argTypes = functionCallExpression.functionCall.arguments.map { getType(it, localVariables) }
        if (formalTypes != argTypes)
            module.errors.add("Function call to '${functionCallExpression.functionCall.functionReference.name}' expected" +
                    " '$formalTypes' but was given '$argTypes'.")
    }

    fun visit(statement: Statement, localVariables: MutableMap<String, Variable>?) {
        when (statement) {
            is VariableDeclarationStatement -> visit(statement, localVariables)
            is VariableReassignmentStatement -> visit(statement, localVariables)
            is FieldSetterStatement -> visit(statement, localVariables)
            is IfStatement -> {
                visit(statement.exp, localVariables)
                assert(T_BOOL == getType(statement.exp, localVariables))
                statement.statements.forEach { visit(it, localVariables) }
                if (statement.elseStatement != null)
                    visit(statement.elseStatement, localVariables)
            }
            is WhileStatement -> {
                visit(statement.exp, localVariables)
                assert(T_BOOL == getType(statement.exp, localVariables))
                statement.statements.forEach { visit(it, localVariables) }
            }
            else -> visit(statement)
        }
    }

    fun visit(fieldSetterStatement: FieldSetterStatement, localVariables: MutableMap<String, Variable>?) {
        val varType = localVariables?.get(fieldSetterStatement.variableReference.name)?.type
        val fieldName = fieldSetterStatement.fieldReference.name
        if (varType is Clazz) {
            val possibleFields = varType.fields.filter { it.name == fieldName }
            val fieldType = possibleFields.last().type
            val expressionType = getType(fieldSetterStatement.expression, localVariables)
            if (fieldType != expressionType)
                module.errors.add("Can't set '$fieldName' to '${fieldSetterStatement.expression}' because it is of type" +
                        " '$expressionType' and it needs to be of type '$fieldType'.")
        } else module.errors.add("Can't set field of '$fieldName' because it is of type '$varType'.")
    }

    fun visit(variableDeclarationStatement: VariableDeclarationStatement, localVariables: MutableMap<String, Variable>?) {
        localVariables?.put(variableDeclarationStatement.variable.name, variableDeclarationStatement.variable)
        visit(variableDeclarationStatement.variable, localVariables)
    }

    fun visit(variableReassignmentStatement: VariableReassignmentStatement, localVariables: MutableMap<String, Variable>?) {
        val name = variableReassignmentStatement.reference.name
        val thingsWithName: MutableList<Variable> = mutableListOf()
        thingsWithName.addAll(module.globalVariables.filter { it.name == name })
        localVariables?.forEach { if (it.key == name) thingsWithName.add(it.value) }

        if (thingsWithName.isNotEmpty()) {
            val variable = thingsWithName.last()
            visit(variableReassignmentStatement.exp, localVariables)
            val expressionType = getType(variableReassignmentStatement.exp, localVariables)
            if (variable.type != expressionType)
                module.errors.add("Variable '${variable.name}' is marked with the type '${variable.type}' but the type" +
                        " of '${variableReassignmentStatement.exp}' is '$expressionType'.")
        }
    }

}