package xyz.jadonfowler.compiler.pass

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function

class SemanticAnalysis(module: Module) : Pass(module) {

    fun getReferences(expression: Expression): List<String> {
        val list = mutableListOf<String>()
        when (expression) {
            is ReferenceExpression -> {
                if (!expression.reference.type.isCopyable()) {
                    list.add(expression.reference.name)
                }
            }
            is BinaryOperator -> {
                list.addAll(getReferences(expression.expressionA))
                list.addAll(getReferences(expression.expressionB))
            }
            is FieldGetterExpression -> {
                list.addAll(getReferences(expression.variable))
            }
        }
        return list
    }

    init {
        module.globalVariables.forEach { visit(it, true) }
        module.globalFunctions.forEach { visit(it) }
        module.globalClasses.forEach { visit(it) }
    }

    override fun visit(variable: Variable) {
        visit(variable, false)
    }

    fun visit(variable: Variable, global: Boolean) {
        if (global) {
            if (!variable.constant)
                reportError("Global variable '${variable.name}' needs to be a constant.", variable.context)

            if (variable.initialExpression == null)
                reportError("Global variable '${variable.name}' needs to be assigned an expression.", variable.context)
        }
    }

    override fun visit(function: Function) {
        val unusedReferences = mutableListOf<String>()
        val usedReferences = mutableListOf<String>()

        function.statements.forEach {
            when (it) {
                is VariableDeclarationStatement -> {
                    val context = it.context
                    it.variable.initialExpression?.let {
                        val refs = getReferences(it)
                        refs.forEach {
                            if (usedReferences.contains(it)) {
                                reportError("Reference to '$it' has already been used.", context)
                            } else {
                                if (unusedReferences.contains(it))
                                    unusedReferences.remove(it)
                                usedReferences.add(it)
                            }
                        }
                    }
                    unusedReferences.add(it.variable.name)
                }
                else -> visit(it)
            }
        }
    }

    override fun visit(fieldSetterStatement: FieldSetterStatement) {
        super.visit(fieldSetterStatement)
    }

}
