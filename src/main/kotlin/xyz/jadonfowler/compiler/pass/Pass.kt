package xyz.jadonfowler.compiler.pass

import org.antlr.v4.runtime.ParserRuleContext
import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import xyz.jadonfowler.compiler.visitor.Visitor

open class Pass(module: Module) : Visitor(module) {

    fun reportError(problem: String, context: ParserRuleContext) {
        val line = context.start.line - 1
        val lines = module.source.split("\n")

        val before = if (line > 0) "$line ${lines[line - 1]}\n" else ""
        val errorLine = "${line + 1} ${lines[line]}\n"
        val after = if (line + 1 < lines.size) "${line + 2} ${lines[line + 1]}\n" else ""

        val column = context.start.charPositionInLine
        val arrow = " ".repeat(line.toString().length + 1) + "~".repeat(column) + "^"
        module.errors.add("$before$errorLine$arrow\n$after$problem\n")
    }

    override fun visit(variable: Variable) {
    }

    override fun visit(variableDeclarationStatement: VariableDeclarationStatement) {
    }

    override fun visit(variableReassignmentStatement: VariableReassignmentStatement) {
    }

    override fun visit(binaryOperator: BinaryOperator) {
    }

    override fun visit(block: Block) {
    }

    override fun visit(clazz: Clazz) {
    }

    override fun visit(falseExpression: FalseExpression) {
    }

    override fun visit(formal: Formal) {
    }

    override fun visit(function: Function) {
    }

    override fun visit(functionCallExpression: FunctionCallExpression) {
    }

    override fun visit(functionCallStatement: FunctionCallStatement) {
    }

    override fun visit(methodCallStatement: MethodCallStatement) {
    }

    override fun visit(methodCallExpression: MethodCallExpression) {
    }

    override fun visit(fieldGetterExpression: FieldGetterExpression) {
    }

    override fun visit(fieldSetterStatement: FieldSetterStatement) {
    }

    override fun visit(ifStatement: IfStatement) {
    }

    override fun visit(integerLiteral: IntegerLiteral) {
    }

    override fun visit(floatLiteral: FloatLiteral) {
    }

    override fun visit(referenceExpression: ReferenceExpression) {
    }

    override fun visit(stringLiteral: StringLiteral) {
    }

    override fun visit(trueExpression: TrueExpression) {
    }

    override fun visit(whileStatement: WhileStatement) {
    }

    override fun visit(clazzInitializerExpression: ClazzInitializerExpression) {
    }
}