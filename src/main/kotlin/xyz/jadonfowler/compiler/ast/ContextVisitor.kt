package xyz.jadonfowler.compiler.ast

import xyz.jadonfowler.compiler.parser.LangBaseVisitor
import xyz.jadonfowler.compiler.parser.LangParser

var globalVariables: MutableList<Variable> = mutableListOf()
var globalFunctions: MutableList<Function> = mutableListOf()
var globalClasses: MutableList<Clazz> = mutableListOf()

/**
 * ContextVisitor transforms a ContextTree into an AST
 */
class ContextVisitor : LangBaseVisitor<Node>() {

    override fun visitProgram(ctx: LangParser.ProgramContext?): Program {
        val externalDeclarations = ctx?.externalDeclaration()

        globalVariables.addAll(externalDeclarations?.filter { it.variableDeclaration() != null }?.map {
            visitVariableDeclaration(it.variableDeclaration())
        }?.filterIsInstance<Variable>().orEmpty())

        globalFunctions.addAll(externalDeclarations?.filter { it.functionDeclaration() != null }?.map {
            visitFunctionDeclaration(it.functionDeclaration())
        }?.filterIsInstance<Function>().orEmpty())

        globalClasses.addAll(externalDeclarations?.filter { it.classDeclaration() != null }?.map {
            visitClassDeclaration(it.classDeclaration())
        }?.filterIsInstance<Clazz>().orEmpty())

        return Program(globalVariables.orEmpty(), globalFunctions.orEmpty(), globalClasses.orEmpty())
    }

    override fun visitExternalDeclaration(ctx: LangParser.ExternalDeclarationContext?): Node {
        if (ctx?.variableDeclaration() != null) return visitVariableDeclaration(ctx?.variableDeclaration())
        if (ctx?.functionDeclaration() != null) return visitFunctionDeclaration(ctx?.functionDeclaration())
        if (ctx?.classDeclaration() != null) return visitClassDeclaration(ctx?.classDeclaration())
        // Should be unreachable
        return EmptyNode()
    }

    override fun visitFunctionDeclaration(ctx: LangParser.FunctionDeclarationContext?): Function {
        val identifier = ctx?.ID()?.symbol?.text.orEmpty()
        val returnType = getType(ctx?.typeAnnotation()?.ID()?.symbol?.text.orEmpty())
        val formals = ctx?.argumentList()?.argument()?.map {
            Formal(getType(it.variableSignature().typeAnnotation().ID().symbol.text),
                    it.variableSignature().ID().symbol.text)
        }.orEmpty()

        val statements = statementListFromContext(ctx?.statementList()).toMutableList()
        val lastStatementContext = ctx?.statement()
        if (lastStatementContext != null) {
            val lastStatement: Node = visitStatement(lastStatementContext)
            if (lastStatement is Statement)
                statements.add(lastStatement)
        }

        var expression: Expression? = null
        val expressionContext = ctx?.expression()
        if (expressionContext != null)
            expression = visitExpression(ctx?.expression())

        return Function(returnType, identifier, formals, statements, expression)
    }

    fun statementListFromContext(statementListContext: LangParser.StatementListContext?): List<Statement> {
        // Create list
        val statements: MutableList<Statement> = mutableListOf(visitStatement(statementListContext?.statement()))
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

    override fun visitVariableDeclaration(ctx: LangParser.VariableDeclarationContext?): Variable {
        val constant: Boolean = ctx?.variableModifier()?.text.equals("let")
        val identifier = ctx?.variableSignature()?.ID()?.symbol?.text.orEmpty()
        val type = getType(ctx?.variableSignature()?.typeAnnotation()?.ID()?.symbol?.text.orEmpty())
        var expression: Expression? = null
        val expressionContext: LangParser.ExpressionContext? = ctx?.expression()
        if (expressionContext != null)
            expression = visitExpression(expressionContext)
        return Variable(type, identifier, expression, constant)
    }

    override fun visitClassDeclaration(ctx: LangParser.ClassDeclarationContext?): Clazz {
        val identifier = ctx?.ID()?.symbol?.text.orEmpty()
        val declarations = ctx?.externalDeclaration()?.map { visitExternalDeclaration(it) }.orEmpty()
        val fields = declarations.filterIsInstance<Variable>()
        val methods = declarations.filterIsInstance<Function>()
        return Clazz(identifier, fields, methods)
    }

    override fun visitFunctionCall(ctx: LangParser.FunctionCallContext?): FunctionCallStatement {
        val functionName = ctx?.ID()?.symbol?.text
        val functions = globalFunctions.filter { it.name.equals(functionName) }
        val expressions = expressionListFromContext(ctx?.expressionList())
        if (functions.size < 1)
            throw IllegalArgumentException("$functionName is not a valid function.")
        return FunctionCallStatement(functions[0], expressions)
    }

    fun expressionListFromContext(expressionListContext: LangParser.ExpressionListContext?): List<Expression> {
        val expressions: MutableList<Expression> = mutableListOf(visitExpression(expressionListContext?.expression()))

        var expressionListContextChild: LangParser.ExpressionListContext? = expressionListContext?.expressionList()
        while (expressionListContextChild != null) {
            val expressionListContextChildExpression = visitExpression(expressionListContextChild.expression())
            expressions.add(expressionListContextChildExpression)
            expressionListContextChild = expressionListContextChild.expressionList()
        }

        return expressions
    }

    override fun visitVariableReassignment(ctx: LangParser.VariableReassignmentContext?): Node {
        // TODO: Return VariableReassignmentStatement
        return EmptyNode()
    }

    override fun visitStatement(ctx: LangParser.StatementContext?): Node /*TODO: return Statement */ {
        if (ctx?.variableDeclaration() != null)
            return VariableDeclarationStatement(visitVariableDeclaration(ctx?.variableDeclaration()))
        if (ctx?.functionCall() != null)
            return visitFunctionCall(ctx?.functionCall())

        val id: String = ctx?.getChild(0)?.text.orEmpty()
        when (id) {
            "if" -> {
                val expression = visitExpression(ctx?.expression())
                val statements = statementListFromContext(ctx?.statementList(0))

                var elseStatement: IfStatement? = null
                if (ctx?.getChild(3)?.text.equals("else"))
                    elseStatement = xyz.jadonfowler.compiler.ast.elseStatement(
                            statementListFromContext(ctx?.getChild(4) as LangParser.StatementListContext))

                return IfStatement(expression, statements, elseStatement)
            }
            else -> return EmptyNode()
        }
    }

    override fun visitExpression(ctx: LangParser.ExpressionContext?): Expression {
        val firstSymbol = ctx?.getChild(0)?.text.orEmpty()
        when (firstSymbol) {
            "(" -> {
                // | '(' expression ')'
                return visitExpression(ctx?.getChild(1) as LangParser.ExpressionContext?)
            }
        }
        if (ctx?.getChild(0) is LangParser.ExpressionContext && ctx?.getChild(2) is LangParser.ExpressionContext) {
            val expressionA = visitExpression(ctx?.getChild(0) as LangParser.ExpressionContext?)
            val expressionB = visitExpression(ctx?.getChild(2) as LangParser.ExpressionContext?)
            val between = ctx?.getChild(1)?.text.orEmpty()
            val operator = getOperator(between)
            if (operator != null) {
                return BinaryOperator(expressionA, operator, expressionB)
            }
        } else if (ctx?.INT() != null) {
            return IntegerLiteral(ctx?.INT()?.text?.toInt()!!)
        } else if (ctx?.ID() != null) {
            val id = ctx?.ID()?.symbol?.text
            val variables = globalVariables.filter { it.name.equals(id) }
            if (variables.size > 0)
                return variables.last().initialExpression!!
        }
        return TrueExpression() // TODO: Remove
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
