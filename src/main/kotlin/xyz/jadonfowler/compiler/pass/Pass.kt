package xyz.jadonfowler.compiler.pass

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import xyz.jadonfowler.compiler.visitor.Visitor

open class Pass(module: Module) : Visitor(module) {
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