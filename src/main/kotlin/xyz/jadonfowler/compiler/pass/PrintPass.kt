package xyz.jadonfowler.compiler.pass

import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function

/**
 * This pass is used for debugging the AST.
 */
class PrintPass(module: Module) : Pass(module) {

    val tab = "    "
    var tabIndent = 0

    var output = ""

    fun printI(any: Any) {
        output += any.toString()
    }

    fun print(any: Any) {
        output += tab.repeat(tabIndent) + any.toString()
    }

    fun println(any: Any) {
        output += tab.repeat(tabIndent) + any.toString() + "\n"
    }

    fun println() {
        println("")
    }

    init {
        println("; Module: ${module.name}")
        module.globalVariables.map { it.accept(this) }
        println()
        module.globalFunctions.map { it.accept(this) }
        println()
        module.globalClasses.map { it.accept(this) }
    }

    override fun visit(function: Function) {
        println("; ${function.name} has ${function.statements.size} statements${if (function.expression != null) " and a return expression" else ""}.")
        print("function ${function.name} (")
        function.formals.map { it.accept(this); if (function.formals.last() != it) print(", ") }
        printI(") -> ${function.returnType} {\n")

        tabIndent++
        function.statements.map { println("; ${it.javaClass.simpleName}"); it.accept(this) }
        if (function.expression != null) {
            print("return ")
            function.expression!!.accept(this)
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
            variable.initialExpression!!.accept(this)
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
        block.statements.map { println("; ${it.javaClass.simpleName}"); it.accept(this) }
        tabIndent--
        println("}")
    }

    override fun visit(statement: Statement) {
        println("; ${statement.javaClass.simpleName}")
        super.visit(statement)
    }

    override fun visit(ifStatement: IfStatement) {
        print("if ")
        ifStatement.exp.accept(this)
        printI(" {\n")
        tabIndent++
        ifStatement.statements.map { println("; ${it.javaClass.simpleName}"); it.accept(this) }
        tabIndent--
        println("}")

        var child: IfStatement? = ifStatement.elseStatement
        while (child != null) {
            print("else if (")
            child.exp.accept(this)
            printI(") {\n")
            tabIndent++
            child.statements.map { println("; ${it.javaClass.simpleName}"); it.accept(this) }
            tabIndent--
            println("}")
            child = child.elseStatement
        }
    }

    override fun visit(whileStatement: WhileStatement) {
        print("while ")
        whileStatement.exp.accept(this)
        printI(" {\n")
        tabIndent++
        whileStatement.statements.map { println("; ${it.javaClass.simpleName}"); it.accept(this) }
        tabIndent--
        println("}")
    }

    override fun visit(variableDeclarationStatement: VariableDeclarationStatement) {
        variableDeclarationStatement.variable.accept(this)
    }

    override fun visit(variableReassignmentStatement: VariableReassignmentStatement) {
        print("${variableReassignmentStatement.reference.name} = ")
        variableReassignmentStatement.exp.accept(this)
        printI("\n")
    }

    override fun visit(functionCallStatement: FunctionCallStatement) {
        print("${functionCallStatement.functionCall.functionReference.name}(")
        functionCallStatement.functionCall.arguments.map { it.accept(this); printI(", ") }
        printI(")\n")
    }

    override fun visit(trueExpression: TrueExpression) {
        printI("true")
    }

    override fun visit(falseExpression: FalseExpression) {
        printI("false")
    }

    override fun visit(fieldGetterExpression: FieldGetterExpression) {
        printI(fieldGetterExpression.variableReference.name + "." + fieldGetterExpression.variableReference.name)
    }

    override fun visit(fieldSetterStatement: FieldSetterStatement) {
        print(fieldSetterStatement.variableReference.name + "." + fieldSetterStatement.variableReference.name + " = ")
        fieldSetterStatement.expression.accept(this)
        println()
    }

    override fun visit(methodCallExpression: MethodCallExpression) {
        printI(methodCallExpression.methodCall.variableReference.name + "."
                + methodCallExpression.methodCall.methodReference.name + "(")
        methodCallExpression.methodCall.arguments.forEach { it.accept(this); printI(", ") }
        printI(")")
    }

    override fun visit(methodCallStatement: MethodCallStatement) {
        print(methodCallStatement.methodCall.variableReference.name + "."
                + methodCallStatement.methodCall.methodReference.name + "(")
        methodCallStatement.methodCall.arguments.forEach { it.accept(this); printI(", ") }
        println(")")
    }

    override fun visit(integerLiteral: IntegerLiteral) {
        printI(integerLiteral.value)
    }

    override fun visit(stringLiteral: StringLiteral) {
        printI("\"${stringLiteral.value}\"")
    }

    override fun visit(referenceExpression: ReferenceExpression) {
        printI(referenceExpression.reference.name)
    }

    override fun visit(functionCallExpression: FunctionCallExpression) {
        printI("${functionCallExpression.functionCall.functionReference.name}(")
        functionCallExpression.functionCall.arguments.forEach { it.accept(this); printI(", ") }
        printI(")")
    }

    override fun visit(binaryOperator: BinaryOperator) {
        printI("(")
        binaryOperator.expA.accept(this)
        printI(" ${binaryOperator.operator.string} ")
        binaryOperator.expB.accept(this)
        printI(")")
    }

}
