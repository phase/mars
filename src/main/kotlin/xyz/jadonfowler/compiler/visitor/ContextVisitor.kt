package xyz.jadonfowler.compiler.visitor

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import xyz.jadonfowler.compiler.globalModules
import xyz.jadonfowler.compiler.parser.LangBaseVisitor
import xyz.jadonfowler.compiler.parser.LangParser

/**
 * ContextVisitor transforms a ContextTree into an AST
 */
class ContextVisitor(val moduleName: String) : LangBaseVisitor<Node>() {

    var globalVariables: MutableList<Variable> = mutableListOf()
    var globalFunctions: MutableList<Function> = mutableListOf()
    var globalClasses: MutableList<Clazz> = mutableListOf()

    override fun visitProgram(ctx: LangParser.ProgramContext?): Module {
        val externalDeclarations = ctx?.externalDeclaration()

        globalVariables.addAll(externalDeclarations?.filter { it.variableDeclaration() != null }?.map {
            visitVariableDeclaration(it.variableDeclaration())
        }.orEmpty())

        globalFunctions.addAll(externalDeclarations?.filter { it.functionDeclaration() != null }?.map {
            visitFunctionDeclaration(it.functionDeclaration())
        }.orEmpty())

        globalClasses.addAll(externalDeclarations?.filter { it.classDeclaration() != null }?.map {
            visitClassDeclaration(it.classDeclaration())
        }.orEmpty())

        val module = Module(moduleName, globalVariables.orEmpty(), globalFunctions.orEmpty(), globalClasses.orEmpty())
        globalModules.add(module)
        return module
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
            val formalType = it.variableSignature().typeAnnotation().ID().symbol.text
            Formal(getType(formalType), formalType, it.variableSignature().ID().symbol.text)
        }.orEmpty()

        val statements = statementListFromStatementListContext(ctx?.statementList()).toMutableList()

        /*
         * (statement | blockStatement)?
         * (statement | blockStatement | expression)?
         */

        if (ctx?.statement() != null && ctx?.statement()!!.size > 0) {
            ctx?.statement()!!.forEach {
                val statementNode = visitStatement(it)
                if (statementNode is Statement)
                    statements.add(statementNode)
            }
        }

        if (ctx?.blockStatement() != null && ctx?.blockStatement()!!.size > 0) {
            ctx?.blockStatement()!!.forEach {
                val blockStatementNode = visitBlockStatement(it)
                if (blockStatementNode is Block)
                    statements.add(blockStatementNode)
            }
        }

        var expression: Expression? = null
        val expressionContext = ctx?.expression()
        if (expressionContext != null)
            expression = visitExpression(ctx?.expression())

        return Function(returnType, identifier, formals, statements, expression)
    }

    fun statementListFromStatementListContext(statementListContext: LangParser.StatementListContext?): List<Statement> {
        var context = statementListContext
        val statements: MutableList<Statement> = mutableListOf()
        while (context != null) {
            var statement: Statement? = null
            val blockStatementContext = context.blockStatement()
            val statementContext = context.statement()
            // There is either a blockStatement or a normal statement, so the order here doesn't matter.
            if (blockStatementContext != null) {
                val blockStatementNode = visitBlockStatement(blockStatementContext)
                if (blockStatementNode is Statement) statement = blockStatementNode
            }
            if (statementContext != null) {
                val statementNode = visitStatement(statementContext)
                if (statementNode is Statement) statement = statementNode
            }
            if (statement != null)
                statements.add(statement)
            // Set current context as child
            context = context.statementList()
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

    override fun visitFunctionCall(ctx: LangParser.FunctionCallContext?): FunctionCall {
        val functionName = ctx?.ID()?.symbol?.text.orEmpty()
        val expressions = expressionListFromContext(ctx?.expressionList())
        return FunctionCall(Reference(functionName), expressions)
    }

    override fun visitField(ctx: LangParser.FieldContext?): FieldExpression {
        return FieldExpression(Reference(ctx?.ID(0)?.symbol?.text.orEmpty()),
                Reference(ctx?.ID(1)?.symbol?.text.orEmpty()))
    }

    override fun visitMethodCall(ctx: LangParser.MethodCallContext?): MethodCall {
        val variableName = ctx?.ID(0)?.symbol?.text.orEmpty()
        val methodName = ctx?.ID(1)?.symbol?.text.orEmpty()
        val expressions = expressionListFromContext(ctx?.expressionList())
        return MethodCall(Reference(variableName), Reference(methodName), expressions)
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

    override fun visitVariableReassignment(ctx: LangParser.VariableReassignmentContext?): VariableReassignmentStatement {
        return VariableReassignmentStatement(Reference(ctx?.ID()?.symbol?.text.orEmpty()), visitExpression(ctx?.expression()))
    }

    override fun visitStatement(ctx: LangParser.StatementContext?): Node /*TODO: return Statement */ {
        if (ctx?.variableDeclaration() != null)
            return VariableDeclarationStatement(visitVariableDeclaration(ctx?.variableDeclaration()))
        if (ctx?.variableReassignment() != null)
            return visitVariableReassignment(ctx?.variableReassignment())
        if (ctx?.methodCall() != null)
            return MethodCallStatement(visitMethodCall(ctx?.methodCall()))
        if (ctx?.functionCall() != null)
            return FunctionCallStatement(visitFunctionCall(ctx?.functionCall()))
        return EmptyNode()
    }

    override fun visitBlockStatement(ctx: LangParser.BlockStatementContext?): Node {
        val id: String = ctx?.getChild(0)?.text.orEmpty()
        return when (id) {
            "if" -> {
                val expression = visitExpression(ctx?.expression())
                val statements = statementListFromStatementListContext(ctx?.statementList(0))

                var elseStatement: IfStatement? = null
                if (ctx?.getChild(3)?.text.equals("else"))
                    elseStatement = elseStatement(
                            statementListFromStatementListContext(ctx?.getChild(4) as LangParser.StatementListContext))

                IfStatement(expression, statements, elseStatement)
            }
            "while" -> {
                val expression = visitExpression(ctx?.expression())
                val statements = statementListFromStatementListContext(ctx?.statementList(0))
                WhileStatement(expression, statements)
            }
            else -> EmptyNode()
        }
    }

    override fun visitExpression(ctx: LangParser.ExpressionContext?): Expression {
        val firstSymbol = ctx?.getChild(0)?.text.orEmpty()
        when (firstSymbol) {
            "(" -> {
                // | '(' expression ')'
                return visitExpression(ctx?.getChild(1) as LangParser.ExpressionContext?)
            }
            "true" -> return TrueExpression()
            "false" -> return FalseExpression()
        }
        if (ctx?.getChild(0) is LangParser.ExpressionContext && ctx?.getChild(2) is LangParser.ExpressionContext) {
            val expressionA = visitExpression(ctx?.getChild(0) as LangParser.ExpressionContext?)
            val expressionB = visitExpression(ctx?.getChild(2) as LangParser.ExpressionContext?)
            val between = ctx?.getChild(1)?.text.orEmpty()
            val operator = getOperator(between)
            if (operator != null) {
                return BinaryOperator(expressionA, operator, expressionB)
            }
        } else if (ctx?.methodCall() != null) {
            return MethodCallExpression(visitMethodCall(ctx?.methodCall()))
        } else if (ctx?.functionCall() != null) {
            return FunctionCallExpression(visitFunctionCall(ctx?.functionCall()))
        } else if (ctx?.field() != null) {
            return visitField(ctx?.field())
        } else if (ctx?.INT() != null) {
            return IntegerLiteral(ctx?.INT()?.text?.toInt()!!)
        } else if (ctx?.ID() != null) {
            val id = ctx?.ID()?.symbol?.text.orEmpty()
            return ReferenceExpression(Reference(id))
        } else if (ctx?.STRING() != null) {
            val value = ctx?.STRING()?.symbol?.text.orEmpty()
            // Remove quotes around string
            return StringLiteral(value.substring(1..value.length - 2))
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
