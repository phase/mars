package xyz.jadonfowler.compiler.backend

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import xyz.jadonfowler.compiler.visitor.Visitor
import java.io.File

abstract class Backend(module: Module) : Visitor(module) {

    abstract fun output(file: File)

    override fun visit(function: Function) {
    }

    override fun visit(formal: Formal) {
    }

    override fun visit(variable: Variable) {
    }

    override fun visit(clazz: Clazz) {
    }

    override fun visit(block: Block) {
    }

    override fun visit(ifStatement: IfStatement) {
    }

    override fun visit(whileStatement: WhileStatement) {
    }

    override fun visit(variableDeclarationStatement: VariableDeclarationStatement) {
    }

    override fun visit(functionCallStatement: FunctionCallStatement) {
    }

    override fun visit(trueExpression: TrueExpression) {
    }

    override fun visit(falseExpression: FalseExpression) {
    }

    override fun visit(integerLiteral: IntegerLiteral) {
    }

    override fun visit(stringLiteral: StringLiteral) {
    }

    override fun visit(referenceExpression: ReferenceExpression) {
    }

    override fun visit(functionCallExpression: FunctionCallExpression) {
    }

    override fun visit(binaryOperator: BinaryOperator) {
    }

}
