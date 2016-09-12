package xyz.jadonfowler.compiler.ast.visitor

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function

class Printer : Visitor() {

    val tab = "    "
    var tabIndent = 0

    fun print(any: Any) = System.out.println(tab.repeat(tabIndent) + any)
    fun println(any: Any) = System.out.println(tab.repeat(tabIndent) + any)

    override fun visit(program: Program) {
        program.globalVariables.map { it.accept(this) }
        program.globalFunctions.map { it.accept(this) }
    }

    override fun visit(function: Function) {
        println("${function.name} (${function.formals.map { it.accept(this); print(" -> ") }}${function.returnType}) {")
        tabIndent++
        function.statements.map { it.accept(this) }
        tabIndent--
        println("}\n")
    }

    override fun visit(formal: Formal) {
        print("(${formal.name} : ${formal.type})")
    }

    override fun visit(variable: Variable) {
        println("var ${variable.name} : ${variable.type}")
    }

    override fun visit(block: Block) {
        println("{")
        tabIndent++
        block.statements.map { it.accept(this) }
        tabIndent--
        println("}")
    }

    override fun visit(ifStatement: IfStatement) {
        println("if ${ifStatement.exp.accept(this)} {")
        tabIndent++
        ifStatement.statements.map { it.accept(this) }
        tabIndent--
        println("}")

        var child: IfStatement? = ifStatement.elseStatement
        while (child != null) {
            println("else if ${child.exp.accept(this)} {")
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

    override fun visit(trueExpression: TrueExpression) {
        print("true")
    }

    override fun visit(falseExpression: FalseExpression) {
        print("false")
    }

    override fun visit(integerLiteral: IntegerLiteral) {
        print(integerLiteral.value)
    }

    override fun visit(identifierExpression: IdentifierExpression) {
        print(identifierExpression.identifier)
    }

    override fun visit(binaryOperator: BinaryOperator) {
        print("(${binaryOperator.expA.accept(this)} ${binaryOperator.operator.string} ${binaryOperator.expB.accept(this)})")
    }
}