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
        }?.filterIsInstance<Variable>().orEmpty()

        val functions = externalDeclarations?.filter { it.functionDeclaration() != null }?.map {
            visitFunctionDeclaration(it.functionDeclaration())
        }?.filterIsInstance<Function>().orEmpty()

        return Program(variables, functions)
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
        }.orEmpty()
        val statements = statementListFromContext(ctx?.statementList())

        return Function(returnType, identifier, formals, statements)
    }

    fun statementListFromContext(statementListContext: LangParser.StatementListContext?) : List<Statement> {
        // Create list
        val statements: MutableList<Statement> = mutableListOf(visit(statementListContext?.statement()))
                .filterIsInstance<Statement>().toMutableList()

        // Append children to list
        var statementListContextChild: LangParser.StatementListContext? = statementListContext?.statementList()
        while (statementListContextChild != null) {
            // Check if Node returned from visit is really a Statement
            val statementListContextChildStatement: Node = visitStatement(statementListContextChild.statement())
            if (statementListContextChildStatement is Statement)
                statements.add(statementListContextChildStatement)
            statementListContextChild = statementListContextChild.statementList()
        }

        return statements
    }

    override fun visitVariableDeclaration(ctx: LangParser.VariableDeclarationContext?): Node {
        val identifier = ctx?.variableSignature()?.ID()?.symbol?.text.orEmpty()
        val type = getType(ctx?.variableSignature()?.typeAnnotation()?.ID()?.symbol?.text.orEmpty())
        // TODO: Initialize variables with expressions
        return Variable(type, identifier)
    }

    override fun visitFunctionCall(ctx: LangParser.FunctionCallContext?): Node {
        // TODO: Return FunctionCallStatement
        return EmptyNode()
    }

    override fun visitVariableReassignment(ctx: LangParser.VariableReassignmentContext?): Node {
        // TODO: Return VariableReassignmentStatement
        return EmptyNode()
    }

    override fun visitStatement(ctx: LangParser.StatementContext?): Node {
        val id: String = ctx?.getChild(0)?.text.orEmpty()
        when(id) {
            "if" -> {
                val expression = visitExpression(ctx?.expression()) as Expression
                val statements = statementListFromContext(ctx?.statementList(0))
                return IfStatement(expression, statements, null)
            }
            else -> return EmptyNode()
        }
    }

    override fun visitExpressionList(ctx: LangParser.ExpressionListContext?): Node {
        return EmptyNode()
    }

    override fun visitExpression(ctx: LangParser.ExpressionContext?): Node {
        if(ctx?.INT() != null) {
            return IntegerLiteral(ctx?.INT()?.text?.toInt()!!)
        }
        return TrueExpression()
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
