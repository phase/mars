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

        return Program(variables.orEmpty(), functions.orEmpty())
    }

    override fun visitExternalDeclaration(ctx: LangParser.ExternalDeclarationContext?): Node {
        if (ctx?.variableDeclaration() != null) return visitVariableDeclaration(ctx?.variableDeclaration())
        if (ctx?.functionDeclaration() != null) return visitFunctionDeclaration(ctx?.functionDeclaration())
        // Should be unreachable
        return EmptyNode()
    }

    override fun visitFunctionDeclaration(ctx: LangParser.FunctionDeclarationContext?): Node {
        val identifier = ctx?.ID()?.symbol?.text.orEmpty()
        val returnType = getType(ctx?.typeAnnotation()?.ID()?.symbol?.text.orEmpty())
        val formals = ctx?.argumentList()?.argument()?.map {
            Formal(getType(it.variableSignature().typeAnnotation().ID().symbol.text),
                    it.variableSignature().ID().symbol.text)
        }

        // Create list
        val statementListContext: LangParser.StatementListContext? = ctx?.statementList()
        val statements: MutableList<Statement> = mutableListOf(visit(statementListContext?.statement()))
                .filterIsInstance<Statement>().toMutableList()

        // Append children to list
        var statementListContextChild: LangParser.StatementListContext? = statementListContext?.statementList()
        while (statementListContextChild != null) {
            // Check if Node returned from visit is really a Statement
            val statementListContextChildStatement: Node = visit(statementListContextChild.statement())
            if (statementListContextChildStatement is Statement)
                statements.add(statementListContextChildStatement)
            statementListContextChild = statementListContextChild.statementList()
        }

        return Function(returnType, identifier, formals.orEmpty(), statements)
    }

    override fun visitVariableDeclaration(ctx: LangParser.VariableDeclarationContext?): Node {
        val identifier = ctx?.variableSignature()?.ID()?.symbol?.text.orEmpty()
        val type = getType(ctx?.variableSignature()?.typeAnnotation()?.ID()?.symbol?.text.orEmpty())
        // TODO: Initialize variables with expressions
        return Variable(type, identifier)
    }

    override fun visitFunctionCall(ctx: LangParser.FunctionCallContext?): Node {
        return EmptyNode()
    }

    override fun visitVariableModifier(ctx: LangParser.VariableModifierContext?): Node {
        return EmptyNode()
    }

    override fun visitVariableReassignment(ctx: LangParser.VariableReassignmentContext?): Node {
        return EmptyNode()
    }

    override fun visitStatement(ctx: LangParser.StatementContext?): Node {
        return EmptyNode()
    }

    override fun visitStatementList(ctx: LangParser.StatementListContext?): Node {
        return EmptyNode()
    }

    override fun visitExpressionList(ctx: LangParser.ExpressionListContext?): Node {
        return EmptyNode()
    }

    override fun visitExpression(ctx: LangParser.ExpressionContext?): Node {
        return EmptyNode()
    }

    override fun visitArgumentList(ctx: LangParser.ArgumentListContext?): Node {
        return EmptyNode()
    }

    override fun visitArgument(ctx: LangParser.ArgumentContext?): Node {
        return EmptyNode()
    }

    override fun visitTypeAnnotation(ctx: LangParser.TypeAnnotationContext?): Node {
        return EmptyNode()
    }

    override fun visitVariableSignature(ctx: LangParser.VariableSignatureContext?): Node {
        return EmptyNode()
    }
}
