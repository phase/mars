package xyz.jadonfowler.compiler.visitor

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import xyz.jadonfowler.compiler.globalModules
import xyz.jadonfowler.compiler.parser.LangBaseVisitor
import xyz.jadonfowler.compiler.parser.LangParser

/**
 * ASTBuilder transforms a ContextTree into an AST
 */
class ASTBuilder(val moduleName: String) : LangBaseVisitor<Node>() {

    val imports: MutableList<Import> = mutableListOf()
    val globalVariables: MutableList<Variable> = mutableListOf()
    val globalFunctions: MutableList<Function> = mutableListOf()
    val globalClasses: MutableList<Clazz> = mutableListOf()

    override fun visitProgram(ctx: LangParser.ProgramContext?): Module {
        val externalDeclarations = ctx?.externalDeclaration()

        imports.addAll(ctx?.importDeclaration()?.map { visitImportDeclaration(it) }.orEmpty())

        globalVariables.addAll(externalDeclarations?.filter { it.variableDeclaration() != null }?.map {
            visitVariableDeclaration(it.variableDeclaration())
        }.orEmpty())

        globalClasses.addAll(externalDeclarations?.filter { it.classDeclaration() != null }?.map {
            visitClassDeclaration(it.classDeclaration())
        }.orEmpty())

        globalFunctions.addAll(externalDeclarations?.filter { it.functionDeclaration() != null }?.map {
            visitFunctionDeclaration(it.functionDeclaration())
        }.orEmpty())

        val module = Module(moduleName, imports, globalVariables, globalFunctions, globalClasses)
        globalModules.add(module)
        return module
    }

    override fun visitImportDeclaration(ctx: LangParser.ImportDeclarationContext?): Import {
        return Import(Reference(ctx?.ID()?.symbol?.text.orEmpty()))
    }

    override fun visitExternalDeclaration(ctx: LangParser.ExternalDeclarationContext?): Node {
        if (ctx?.variableDeclaration() != null) return visitVariableDeclaration(ctx?.variableDeclaration())
        if (ctx?.functionDeclaration() != null) return visitFunctionDeclaration(ctx?.functionDeclaration())
        if (ctx?.classDeclaration() != null) return visitClassDeclaration(ctx?.classDeclaration())
        // Should be unreachable
        return EmptyNode()
    }

    override fun visitFunctionDeclaration(ctx: LangParser.FunctionDeclarationContext?): Function {
        var identifier = ctx?.ID()?.symbol?.text.orEmpty()

        // replace `main` with `real_main` for wrapping
        if (identifier == "main")
            identifier = "real_main"

        val returnType = getType(ctx?.typeAnnotation()?.ID()?.symbol?.text.orEmpty(), globalClasses)
        val formals = ctx?.argumentList()?.argument()?.map {
            Formal(getType(it.variableSignature()?.typeAnnotation()?.ID()?.symbol?.text.orEmpty(), globalClasses),
                    it.variableSignature()?.ID()?.symbol?.text.orEmpty())
        }.orEmpty()

        val statements = statementListFromStatementListContext(ctx?.statementList()).toMutableList()

        if (ctx?.statement() != null) {
            val statementNode = visitStatement(ctx?.statement())
            if (statementNode is Statement)
                statements.add(statementNode)
        }

        if (ctx?.blockStatement() != null) {
            val blockStatementNode = visitBlockStatement(ctx?.blockStatement())
            if (blockStatementNode is Block)
                statements.add(blockStatementNode)
        }

        var expression: Expression? = null
        val expressionContext = ctx?.expression()
        if (expressionContext != null)
            expression = visitExpression(ctx?.expression())

        val attributes = attributeListFromAttributeListContext(ctx?.attributeList())

        return Function(attributes, returnType, identifier, formals, statements, expression)
    }

    fun attributeListFromAttributeListContext(attributeListContext: LangParser.AttributeListContext?): List<Attribute> {
        var context = attributeListContext
        val attributes: MutableList<Attribute> = mutableListOf()
        while (context != null && context.attribute() != null) {
            attributes.add(visitAttribute(context.attribute()))
            context = context.attributeList()
        }
        return attributes
    }

    override fun visitAttribute(ctx: LangParser.AttributeContext?): Attribute {
        val name = ctx?.ID(0)?.symbol?.text.orEmpty()
        val values: MutableList<String> = mutableListOf()
        (1..ctx?.ID()?.size!! - 1).forEach {
            values.add(ctx?.ID(it)?.symbol?.text.orEmpty())
        }
        return Attribute(name, values)
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
        val type = getType(ctx?.variableSignature()?.typeAnnotation()?.ID()?.symbol?.text.orEmpty(), globalClasses)
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

    override fun visitMethodCall(ctx: LangParser.MethodCallContext?): MethodCall {
        val variableName = ctx?.ID(0)?.symbol?.text.orEmpty()
        val methodName = ctx?.ID(1)?.symbol?.text.orEmpty()
        val expressions = expressionListFromContext(ctx?.expressionList())
        return MethodCall(Reference(variableName), Reference(methodName), expressions)
    }

    override fun visitFieldGetter(ctx: LangParser.FieldGetterContext?): FieldGetterExpression {
        val variableReference = Reference(ctx?.ID(0)?.symbol?.text.orEmpty())
        val fieldReference = Reference(ctx?.ID(1)?.symbol?.text.orEmpty())
        return FieldGetterExpression(variableReference, fieldReference)
    }

    override fun visitFieldSetter(ctx: LangParser.FieldSetterContext?): FieldSetterStatement {
        val variableReference = Reference(ctx?.ID(0)?.symbol?.text.orEmpty())
        val fieldReference = Reference(ctx?.ID(1)?.symbol?.text.orEmpty())
        val expression = visitExpression(ctx?.expression())
        return FieldSetterStatement(variableReference, fieldReference, expression)
    }

    fun expressionListFromContext(expressionListContext: LangParser.ExpressionListContext?): List<Expression> {
        val expressions: MutableList<Expression> = if (expressionListContext?.expression() != null)
            mutableListOf(visitExpression(expressionListContext?.expression()))
        else mutableListOf()

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
        if (ctx?.fieldSetter() != null)
            return visitFieldSetter(ctx?.fieldSetter())
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
                val statements = statementListFromStatementListContext(ctx?.statementList())

                val parentIf: IfStatement = IfStatement(expression, statements, null)
                var currentIf: IfStatement = parentIf

                ctx?.elif()?.forEach {
                    val elifExpression = visitExpression(it.expression())
                    val elifStatements = statementListFromStatementListContext(it.statementList())
                    val elif = IfStatement(elifExpression, elifStatements, null)
                    currentIf.elseStatement = elif
                    currentIf = elif
                }

                if (ctx?.elseStatement() != null) {
                    val elseStatements = statementListFromStatementListContext(ctx?.elseStatement()?.statementList())
                    currentIf.elseStatement = elseStatement(elseStatements)
                }

                parentIf
            }
            "while" -> {
                val expression = visitExpression(ctx?.expression())
                val statements = statementListFromStatementListContext(ctx?.statementList())
                WhileStatement(expression, statements)
            }
            else -> EmptyNode()
        }
    }

    override fun visitExpression(ctx: LangParser.ExpressionContext?): Expression {
        val firstSymbol = ctx?.getChild(0)?.text.orEmpty()
        val secondSymbol = ctx?.getChild(1)?.text.orEmpty()
        when (firstSymbol) {
            "(" -> {
                // | '(' expression ')'
                return visitExpression(ctx?.getChild(1) as LangParser.ExpressionContext?)
            }
            "true" -> return TrueExpression()
            "false" -> return FalseExpression()
        }
        when (secondSymbol) {
            "`" -> {
                val firstExpression = visitExpression(ctx?.getChild(0) as LangParser.ExpressionContext?)
                val secondExpression = visitExpression(ctx?.getChild(4) as LangParser.ExpressionContext?)
                val functionName = ctx?.getChild(2)?.text.orEmpty()
                return FunctionCallExpression(FunctionCall(Reference(functionName), listOf(firstExpression, secondExpression)))
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
        } else if (ctx?.methodCall() != null) {
            return MethodCallExpression(visitMethodCall(ctx?.methodCall()))
        } else if (ctx?.fieldGetter() != null) {
            return visitFieldGetter(ctx?.fieldGetter())
        } else if (ctx?.functionCall() != null) {
            return FunctionCallExpression(visitFunctionCall(ctx?.functionCall()))
        } else if (ctx?.classInitializer() != null) {
            return visitClassInitializer(ctx?.classInitializer())
        } else if (ctx?.INT() != null) {
            return IntegerLiteral(ctx?.INT()?.text?.toInt()!!)
        } else if (ctx?.FLOAT() != null) {
            val text = ctx?.FLOAT()?.text.orEmpty()
            if (text.endsWith("d"))
            // Remove double suffix
                return FloatLiteral(text.substring(0..text.length - 2).toDouble(), T_FLOAT64)
            else if (text.endsWith("q"))
                return FloatLiteral(text.substring(0..text.length - 2).toDouble(), T_FLOAT128)
            else
                return FloatLiteral(text.toDouble(), T_FLOAT32)
        } else if (ctx?.ID() != null) {
            val id = ctx?.ID()?.symbol?.text.orEmpty()
            return ReferenceExpression(Reference(id))
        } else if (ctx?.STRING() != null) {
            val value = ctx?.STRING()?.symbol?.text.orEmpty()
            // Remove quotes around string
            return StringLiteral(value.substring(1..value.length - 2))
        }
        throw Exception("${ctx?.text} can't be handled yet")
    }

    override fun visitClassInitializer(ctx: LangParser.ClassInitializerContext?): ClazzInitializerExpression {
        val className = ctx?.ID()?.symbol?.text.orEmpty()
        val expressions = expressionListFromContext(ctx?.expressionList())
        return ClazzInitializerExpression(Reference(className), expressions)
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
