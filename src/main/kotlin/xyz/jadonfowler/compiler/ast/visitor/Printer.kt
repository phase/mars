package xyz.jadonfowler.compiler.ast.visitor

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function

class Printer : Visitor() {

    val tab = "    "
    var tabIndent = 0

    fun printI(any: Any) = System.out.print(any)
    fun print(any: Any) = System.out.print(tab.repeat(tabIndent) + any)
    fun println(any: Any) = System.out.println(tab.repeat(tabIndent) + any)

    override fun visit(module: Module) {
        module.globalVariables.map { it.accept(this) }
        println()
        module.globalFunctions.map { it.accept(this) }
        println()
        module.globalClasses.map { it.accept(this) }
    }

    override fun visit(function: Function) {
        println("// ${function.name} has ${function.statements.size} statements.")
        print("function ${function.name} (")
        function.formals.map { it.accept(this); if (!function.formals.last().equals(it)) print(", ") }
        printI(") -> ${function.returnType} {\n")

        tabIndent++
        function.statements.map { it.accept(this) }
        if (function.expression != null) {
            print("return ")
            function.expression.accept(this)
            println()
        }
        tabIndent--

        println("}\n")
    }

    override fun visit(formal: Formal) {
        printI("${formal.name}: ${formal.type}")
    }

    override fun visit(variable: Variable) {
        print("${if (variable.constant) "constant" else "variable"} ${variable.name}: ${variable.type}")
        if (variable.initialExpression != null) {
            printI(" = ")
            variable.initialExpression.accept(this)
        }
        printI("\n")
    }

    override fun visit(clazz: Clazz) {
        println("class ${clazz.name} {")
        tabIndent++
        clazz.fields.map { it.accept(this) }
        println()
        clazz.methods.map { it.accept(this) }
        tabIndent--
        println("}")
    }

    override fun visit(block: Block) {
        println("{")
        tabIndent++
        block.statements.map { it.accept(this) }
        tabIndent--
        println("}")
    }

    override fun visit(ifStatement: IfStatement) {
        print("if ")
        ifStatement.exp.accept(this)
        printI(" {\n")
        tabIndent++
        ifStatement.statements.map { it.accept(this) }
        tabIndent--
        println("}")

        var child: IfStatement? = ifStatement.elseStatement
        while (child != null) {
            print("else if (")
            child.exp.accept(this)
            printI(") {\n")
            tabIndent++
            child.statements.map { it.accept(this) }
            tabIndent--
            println("}")
            child = child.elseStatement
        }
    }

    override fun visit(whileStatement: WhileStatement) {
        println("while ${whileStatement.exp.accept(this)} {")
        tabIndent++
        whileStatement.statements.map { it.accept(this) }
        tabIndent--
        println("}")
    }

    override fun visit(variableDeclarationStatement: VariableDeclarationStatement) {
        variableDeclarationStatement.variable.accept(this)
    }

    override fun visit(functionCallStatement: FunctionCallStatement) {
        print("${functionCallStatement.function?.name}(")
        functionCallStatement.arguments.map { it.accept(this); printI(", ") }
        printI(")\n")
    }

    override fun visit(trueExpression: TrueExpression) {
        printI("true")
    }

    override fun visit(falseExpression: FalseExpression) {
        printI("false")
    }

    override fun visit(integerLiteral: IntegerLiteral) {
        printI(integerLiteral.value)
    }

    override fun visit(identifierExpression: IdentifierExpression) {
        printI(identifierExpression.identifier)
    }

    override fun visit(binaryOperator: BinaryOperator) {
        printI("(")
        binaryOperator.expA.accept(this)
        printI(" ${binaryOperator.operator.string} ")
        binaryOperator.expB.accept(this)
        printI(")")
    }
}