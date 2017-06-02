package xyz.jadonfowler.compiler.visitor

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import xyz.jadonfowler.compiler.globalModules
import xyz.jadonfowler.compiler.parser.LangBaseVisitor
import xyz.jadonfowler.compiler.parser.LangParser

/**
 * ASTBuilder transforms a ContextTree into an AST
 */
class ASTBuilder(val moduleName: String, val source: String) : LangBaseVisitor<Node>() {

    val imports: MutableList<Import> = mutableListOf()
    val globalVariables: MutableList<Variable> = mutableListOf()
    val globalFunctions: MutableList<Function> = mutableListOf()
    val globalClasses: MutableList<Clazz> = mutableListOf()

    override fun visitProgram(ctx: LangParser.ProgramContext?): Module {
        val externalDeclarations = ctx?.externalDeclaration()

        imports.addAll(ctx?.importDeclaration()?.map { import(it) }?.flatten()!!)

        globalVariables.addAll(externalDeclarations?.filter { it.variableDeclaration() != null }?.map {
            visitVariableDeclaration(it.variableDeclaration())
        }.orEmpty())

        externalDeclarations?.filter { it.classDeclaration() != null }?.forEach {
            globalClasses.add(visitClassDeclaration(it.classDeclaration()))
        }

        globalFunctions.addAll(externalDeclarations?.filter { it.functionDeclaration() != null }?.map {
            visitFunctionDeclaration(it.functionDeclaration())
        }.orEmpty())

        val module = Module(moduleName, imports, globalVariables, globalFunctions, globalClasses, source)
        globalModules.add(module)
        return module
    }

    fun import(ctx: LangParser.ImportDeclarationContext?): List<Import> {
        return ctx?.id_p()?.map { Import(Reference(it?.text ?: ""), ctx) }.orEmpty()
    }

    override fun visitExternalDeclaration(ctx: LangParser.ExternalDeclarationContext?): Node {
        ctx?.variableDeclaration()?.let { return visitVariableDeclaration(it) }
        ctx?.functionDeclaration()?.let { return visitFunctionDeclaration(it) }
        ctx?.classDeclaration()?.let { return visitClassDeclaration(it) }
        // Should be unreachable
        return EmptyNode()
    }

    fun getFunctionPrototype(ctx: LangParser.FunctionPrototypeContext?): Function.Prototype {
        var identifier = ctx?.ID()?.symbol?.text.orEmpty()

        // replace `main` with `real_main` for wrapping
        if (identifier == "main")
            identifier = "real_main"

        val returnType = getType(ctx?.typeAnnotation()?.ID()?.symbol?.text.orEmpty(), globalClasses)
        val formals = ctx?.argumentList()?.argument()?.map {
            Formal(getType(it.variableSignature()?.typeAnnotation()?.ID()?.symbol?.text.orEmpty(), globalClasses),
                    it.variableSignature()?.ID()?.symbol?.text.orEmpty(), ctx)
        }.orEmpty()

        val attributes = attributeListFromAttributeListContext(ctx?.attributeList())

        return Function.Prototype(attributes, returnType, identifier, formals)
    }

    override fun visitFunctionDeclaration(ctx: LangParser.FunctionDeclarationContext?): Function {

        val statements = statementListFromStatementListContext(ctx?.statementList()).toMutableList()

        ctx?.statement()?.let {
            val statementNode = visitStatement(it)
            if (statementNode is Statement)
                statements.add(statementNode)
        }

        ctx?.blockStatement()?.let {
            val blockStatementNode = visitBlockStatement(it)
            if (blockStatementNode is Block)
                statements.add(blockStatementNode)
        }

        var expression: Expression? = null
        ctx?.expression()?.let { expression = visitExpression(it) }


        return Function(getFunctionPrototype(ctx?.functionPrototype()), statements, expression, ctx!!)
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
        return Attribute(name, values, ctx!!)
    }

    fun statementListFromStatementListContext(statementListContext: LangParser.StatementListContext?): List<Statement> {
        var context = statementListContext
        val statements: MutableList<Statement> = mutableListOf()
        while (context != null) {
            var statement: Statement? = null
            val blockStatementContext = context.blockStatement()
            val statementContext = context.statement()
            // There is either a blockStatement or a normal statement, so the order here doesn't matter.
            blockStatementContext?.let {
                val blockStatementNode = visitBlockStatement(it)
                if (blockStatementNode is Statement) statement = blockStatementNode
            }
            statementContext?.let {
                val statementNode = visitStatement(it)
                if (statementNode is Statement) statement = statementNode
            }
            statement?.let {
                statements.add(it)
            }
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
        ctx?.expression()?.let {
            expression = visitExpression(it)
        }
        return Variable(type, identifier, expression, constant, ctx!!)
    }

    override fun visitClassDeclaration(ctx: LangParser.ClassDeclarationContext?): Clazz {
        val identifier = ctx?.ID()?.symbol?.text.orEmpty()
        val declarations = ctx?.externalDeclaration()?.map { visitExternalDeclaration(it) }.orEmpty()
        val fields = declarations.filterIsInstance<Variable>()
        val methods = declarations.filterIsInstance<Function>().toMutableList()
        val constructors = methods.filter { it.name == "init" }
        val constructor: Function? = if (constructors.isNotEmpty()) constructors.last() else null
        constructor?.let {
            methods.remove(constructor)
        }
        return Clazz(identifier, fields, methods, constructor, ctx!!)
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

    override fun visitFieldSetter(ctx: LangParser.FieldSetterContext?): FieldSetterStatement {
        val variable = visitExpression(ctx?.expression(0))
        val fieldReference = Reference(ctx?.ID()?.symbol?.text.orEmpty())
        val expression = visitExpression(ctx?.expression(1))
        return FieldSetterStatement(variable, fieldReference, expression, ctx!!)
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
        return VariableReassignmentStatement(Reference(ctx?.ID()?.symbol?.text.orEmpty()), visitExpression(ctx?.expression()), ctx!!)
    }

    override fun visitStatement(ctx: LangParser.StatementContext?): Node /*TODO: return Statement */ {
        ctx?.fieldSetter()?.let {
            return visitFieldSetter(it)
        }
        ctx?.variableDeclaration()?.let {
            return VariableDeclarationStatement(visitVariableDeclaration(it), ctx)
        }
        ctx?.variableReassignment()?.let {
            return visitVariableReassignment(it)
        }
        ctx?.methodCall()?.let {
            return MethodCallStatement(visitMethodCall(it), ctx)
        }
        ctx?.functionCall()?.let {
            return FunctionCallStatement(visitFunctionCall(it), ctx)
        }
        return EmptyNode()
    }

    override fun visitBlockStatement(ctx: LangParser.BlockStatementContext?): Node {
        val id: String = ctx?.getChild(0)?.text.orEmpty()
        return when (id) {
            "if" -> {
                val expression = visitExpression(ctx?.expression())
                val statements = statementListFromStatementListContext(ctx?.statementList())

                val parentIf: IfStatement = IfStatement(expression, statements, null, ctx!!)
                var currentIf: IfStatement = parentIf

                ctx.elif()?.forEach {
                    val elifExpression = visitExpression(it.expression())
                    val elifStatements = statementListFromStatementListContext(it.statementList())
                    val elif = IfStatement(elifExpression, elifStatements, null, ctx)
                    currentIf.elseStatement = elif
                    currentIf = elif
                }

                ctx.elseStatement()?.let {
                    val elseStatements = statementListFromStatementListContext(it.statementList())
                    currentIf.elseStatement = elseStatement(elseStatements, ctx)
                }

                parentIf
            }
            "while" -> {
                val expression = visitExpression(ctx?.expression())
                val statements = statementListFromStatementListContext(ctx?.statementList())
                WhileStatement(expression, statements, ctx!!)
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
            "true" -> return TrueExpression(ctx!!)
            "false" -> return FalseExpression(ctx!!)
        }
        when (secondSymbol) {
            "`" -> {
                // Inline Function Call
                val firstExpression = visitExpression(ctx?.getChild(0) as LangParser.ExpressionContext?)
                val secondExpression = visitExpression(ctx?.getChild(4) as LangParser.ExpressionContext?)
                val functionName = ctx?.getChild(2)?.text.orEmpty()
                return FunctionCallExpression(FunctionCall(Reference(functionName), listOf(firstExpression, secondExpression)), ctx!!)
            }
            "." -> {
                // Field Getter
                val variable = visitExpression(ctx?.expression(0))
                val fieldReference = Reference(ctx?.ID()?.symbol?.text.orEmpty())
                return FieldGetterExpression(variable, fieldReference, ctx!!)
            }
        }
        if (ctx?.getChild(0) is LangParser.ExpressionContext && ctx?.getChild(2) is LangParser.ExpressionContext) {
            val expressionA = visitExpression(ctx?.getChild(0) as LangParser.ExpressionContext?)
            val expressionB = visitExpression(ctx?.getChild(2) as LangParser.ExpressionContext?)
            val between = ctx?.getChild(1)?.text.orEmpty()
            val operator = getOperator(between)
            operator?.let {
                return BinaryOperator(expressionA, it, expressionB, ctx!!)
            }
        }
        ctx?.methodCall()?.let {
            return MethodCallExpression(visitMethodCall(it), ctx)
        }
        ctx?.functionCall()?.let {
            return FunctionCallExpression(visitFunctionCall(it), ctx)
        }
        ctx?.classInitializer()?.let {
            return visitClassInitializer(it)
        }
        ctx?.INT()?.let {
            return IntegerLiteral(it.text?.toInt()!!, ctx)
        }
        ctx?.FLOAT()?.let {
            val text = it.text.orEmpty()
            if (text.endsWith("d"))
            // Remove double suffix
                return FloatLiteral(text.substring(0..text.length - 2).toDouble(), T_FLOAT64, ctx)
            else if (text.endsWith("q"))
                return FloatLiteral(text.substring(0..text.length - 2).toDouble(), T_FLOAT128, ctx)
            else
                return FloatLiteral(text.toDouble(), T_FLOAT32, ctx)
        }
        ctx?.ID()?.let {
            val id = it.symbol?.text.orEmpty()
            return ReferenceExpression(Reference(id), ctx)
        }
        ctx?.STRING()?.let {
            val value = it.symbol?.text.orEmpty()
            // Remove quotes around string
            return StringLiteral(value.substring(1..value.length - 2), ctx)
        }
        throw Exception("${ctx?.text} can't be handled yet")
    }

    override fun visitClassInitializer(ctx: LangParser.ClassInitializerContext?): ClazzInitializerExpression {
        val className = ctx?.ID()?.symbol?.text.orEmpty()
        val expressions = expressionListFromContext(ctx?.expressionList())
        return ClazzInitializerExpression(Reference(className), expressions, ctx!!)
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
