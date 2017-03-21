package xyz.jadonfowler.compiler.pass

import xyz.jadonfowler.compiler.ast.Module
import xyz.jadonfowler.compiler.ast.Variable

class SemanticAnalysis(module: Module) : Pass(module) {

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

            variable.initialExpression?.let {
                visit(it)
            }
        }
    }

}
