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
        module.globalVariables.map { visit(it) }
        println()
        module.globalFunctions.map { visit(it) }
        println()
        module.globalClasses.map { visit(it) }
    }

    override fun visit(function: Function) {
        println("; ${function.name} has ${function.statements.size} statements${if (function.expression != null) " and a return expression" else ""}.")
        print("function ${function.name} (")
        function.formals.map { visit(it); if (function.formals.last() != it) print(", ") }
        printI(") -> ${function.returnType} {\n")

        tabIndent++
        function.statements.map { println("; ${it.javaClass.simpleName}"); visit(it) }
        if (function.expression != null) {
            print("return ")
            visit(function.expression!!)
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
            visit(variable.initialExpression!!)
        }
        printI("\n")
    }

    override fun visit(clazz: Clazz) {
        println("class ${clazz.name} {")
        tabIndent++
        clazz.fields.map { visit(it) }
        println()
        clazz.methods.map { visit(it) }
        tabIndent--
        println("}")
    }

    override fun visit(block: Block) {
        println("{")
        tabIndent++
        block.statements.map { println("; ${it.javaClass.simpleName}"); visit(it) }
        tabIndent--
        println("}")
    }

    override fun visit(statement: Statement) {
        println("; ${statement.javaClass.simpleName}")
        super.visit(statement)
    }

    override fun visit(ifStatement: IfStatement) {
        print("if ")
        visit(ifStatement.expression)
        printI(" {\n")
        tabIndent++
        ifStatement.statements.map { println("; ${it.javaClass.simpleName}"); visit(it) }
        tabIndent--
        println("}")

        var child: IfStatement? = ifStatement.elseStatement
        while (child != null) {
            print("else if (")
            visit(child.expression)
            printI(") {\n")
            tabIndent++
            child.statements.map { println("; ${it.javaClass.simpleName}"); visit(it) }
            tabIndent--
            println("}")
            child = child.elseStatement
        }
    }

    override fun visit(whileStatement: WhileStatement) {
        print("while ")
        visit(whileStatement.expression)
        printI(" {\n")
        tabIndent++
        whileStatement.statements.map { println("; ${it.javaClass.simpleName}"); visit(it) }
        tabIndent--
        println("}")
    }

    override fun visit(variableDeclarationStatement: VariableDeclarationStatement) {
        visit(variableDeclarationStatement.variable)
    }

    override fun visit(variableReassignmentStatement: VariableReassignmentStatement) {
        print("${variableReassignmentStatement.reference.name} = ")
        visit(variableReassignmentStatement.expression)
        printI("\n")
    }

    override fun visit(functionCallStatement: FunctionCallStatement) {
        print("${functionCallStatement.functionCall.functionReference.name}(")
        functionCallStatement.functionCall.arguments.map { visit(it); printI(", ") }
        printI(")\n")
    }

    override fun visit(trueExpression: TrueExpression) {
        printI("true")
    }

    override fun visit(falseExpression: FalseExpression) {
        printI("false")
    }

    override fun visit(fieldGetterExpression: FieldGetterExpression) {
        printI(fieldGetterExpression.variableReference.name + "." + fieldGetterExpression.fieldReference.name)
    }

    override fun visit(fieldSetterStatement: FieldSetterStatement) {
        print(fieldSetterStatement.variableReference.name + "." + fieldSetterStatement.fieldReference.name + " = ")
        visit(fieldSetterStatement.expression)
        println()
    }

    override fun visit(methodCallExpression: MethodCallExpression) {
        printI(methodCallExpression.methodCall.variableReference.name + "."
                + methodCallExpression.methodCall.methodReference.name + "(")
        methodCallExpression.methodCall.arguments.forEach { visit(it); printI(", ") }
        printI(")")
    }

    override fun visit(methodCallStatement: MethodCallStatement) {
        print(methodCallStatement.methodCall.variableReference.name + "."
                + methodCallStatement.methodCall.methodReference.name + "(")
        methodCallStatement.methodCall.arguments.forEach { visit(it); printI(", ") }
        println(")")
    }

    override fun visit(integerLiteral: IntegerLiteral) {
        printI(integerLiteral.value)
    }

    override fun visit(floatLiteral: FloatLiteral) {
        printI(floatLiteral.value)
    }

    override fun visit(stringLiteral: StringLiteral) {
        printI("\"${stringLiteral.value}\"")
    }

    override fun visit(referenceExpression: ReferenceExpression) {
        printI(referenceExpression.reference.name)
    }

    override fun visit(functionCallExpression: FunctionCallExpression) {
        printI("${functionCallExpression.functionCall.functionReference.name}(")
        functionCallExpression.functionCall.arguments.forEach { visit(it); printI(", ") }
        printI(")")
    }

    override fun visit(clazzInitializerExpression: ClazzInitializerExpression) {
        printI("new ${clazzInitializerExpression.classReference.name}(")
        clazzInitializerExpression.arguments.forEach { visit(it); printI(", ") }
        printI(")")
    }

    override fun visit(binaryOperator: BinaryOperator) {
        printI("(")
        visit(binaryOperator.expressionA)
        printI(" ${binaryOperator.operator.string} ")
        visit(binaryOperator.expressionB)
        printI(")")
    }

}
