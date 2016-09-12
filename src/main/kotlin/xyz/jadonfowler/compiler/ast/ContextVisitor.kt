package xyz.jadonfowler.compiler.ast

import xyz.jadonfowler.compiler.parser.LangBaseVisitor
import xyz.jadonfowler.compiler.parser.LangParser

/**
 * ContextVisitor transforms a ContextTree into an AST
 */
class ContextVisitor : LangBaseVisitor<Node>() {

    override fun visitProgram(ctx: LangParser.ProgramContext?): Node {
        val externalDeclarations = ctx?.externalDeclaration()

        val variables = externalDeclarations?.filter { it.variableDeclaration() != null }?.map {
            visitVariableDeclaration(it.variableDeclaration())
        }?.filterIsInstance<Variable>()

        val functions = externalDeclarations?.filter { it.functionDeclaration() != null }?.map {
            visitFunctionDeclaration(it.functionDeclaration())
        }?.filterIsInstance<Function>()

        return Program(variables!!, functions!!)
    }

    override fun visitExternalDeclaration(ctx: LangParser.ExternalDeclarationContext?): Node {
    }

    override fun visitFunctionDeclaration(ctx: LangParser.FunctionDeclarationContext?): Node {
    }

    override fun visitVariableDeclaration(ctx: LangParser.VariableDeclarationContext?): Node {
    }

    override fun visitFunctionCall(ctx: LangParser.FunctionCallContext?): Node {
    }

    override fun visitVariableModifier(ctx: LangParser.VariableModifierContext?): Node {
    }

    override fun visitVariableReassignment(ctx: LangParser.VariableReassignmentContext?): Node {
    }

    override fun visitStatement(ctx: LangParser.StatementContext?): Node {
    }

    override fun visitStatementList(ctx: LangParser.StatementListContext?): Node {
    }

    override fun visitExpressionList(ctx: LangParser.ExpressionListContext?): Node {
    }

    override fun visitExpression(ctx: LangParser.ExpressionContext?): Node {
    }

    override fun visitArgumentList(ctx: LangParser.ArgumentListContext?): Node {
    }

    override fun visitArgument(ctx: LangParser.ArgumentContext?): Node {
    }

    override fun visitTypeAnnotation(ctx: LangParser.TypeAnnotationContext?): Node {
    }

    override fun visitVariableSignature(ctx: LangParser.VariableSignatureContext?): Node {
    }
}
