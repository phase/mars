package xyz.jadonfowler.compiler.ast

import xyz.jadonfowler.compiler.ast.visitor.Visitor

interface Node

/**
 * Used in ContextVisitor for areas where we need to return something.
 */
class EmptyNode : Node

class Program(val globalVariables: List<Variable>, val globalFunctions: List<Function>) : Node {
    fun accept(visitor: Visitor) = visitor.visit(this)
}

class Function(val returnType: Type, val name: String, val formals: List<Formal>, val statements: List<Statement>) : Node {
    val variableTable = mutableMapOf<Variable, Expression>()
    fun accept(visitor: Visitor) = visitor.visit(this)
}

class Formal(val type: Type, val name: String) : Node {
    fun accept(visitor: Visitor) = visitor.visit(this)
}

class Variable(val type: Type, val name: String, val initialExpression: Expression? = null, val constant: Boolean = false) : Node {
    fun accept(visitor: Visitor) = visitor.visit(this)
}


open class Type(val name: String) {
    override fun toString(): String {
        return name
    }
}

class Void : Type("void")

fun getType(name: String): Type {
    // TODO: Real name lookup
    when (name) {
        "void" -> Void()
    }
    return Type(name)
}

// Statements

abstract class Statement : Node {
    open fun accept(visitor: Visitor) = visitor.visit(this)
}

open class Block(val statements: List<Statement>) : Statement() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * Block with an expression to be evaluated before the statements run
 *
 * <pre>
 *     (expr) {
 *          statements;
 *     }
 * </pre>
 *
 * @param exp Expression to test
 * @param statements Statements to be run
 */
open class CheckedBlock(val exp: Expression, statements: List<Statement>) : Block(statements)

/**
 * If Statement
 * The statement list runs if the expression evaluates to true.
 * If the expression is false, control goes to elseStatement. This statement is an If Statement that has its own
 * expression to evaluate. There is no notion of an "Else Statement", they are If Statements with the expression set to
 * true.
 *
 * <pre>
 *     if (eA) {
 *         sA
 *     }
 *     else if (eB) {
 *         sB
 *     }
 *     else if (eC) {
 *         sC
 *     }
 *     else {
 *         sD
 *     }
 * </pre>
 *
 * This is represented in the tree as:
 *
 * <pre>
 *     IfStatement
 *     - eA
 *     - sA
 *     - IfStatement
 *       - eB
 *       - sB
 *       - IfStatement
 *         - eC
 *         - sC
 *         - IfStatement
 *           - true
 *           - sD
 * </pre>
 */
class IfStatement(exp: Expression, statements: List<Statement>, val elseStatement: IfStatement?) : CheckedBlock(exp, statements) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * elseStatement returns an IfStatement with the expression set to a TrueExpression
 * @param statements Statements to run
 */
fun elseStatement(statements: List<Statement>): IfStatement {
    return IfStatement(TrueExpression(), statements, null)
}

class WhileStatement(exp: Expression, statements: List<Statement>) : CheckedBlock(exp, statements) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

class VariableDeclarationStatement(val variable: Variable) : Statement() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

class FunctionCallStatement(val function: Function?, val arguments: List<Expression> = listOf()) : Statement() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

// Expressions

abstract class Expression(val child: List<Expression> = listOf()) : Node {
    open fun accept(visitor: Visitor) = visitor.visit(this)
}

class TrueExpression : Expression() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

class FalseExpression : Expression() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

class IntegerLiteral(val value: Int) : Expression() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

class IdentifierExpression(val identifier: String) : Expression() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}


// Binary Operators
enum class Operator(val string: String) {
    // Maths
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),

    // Comparisons
    GREATER_THAN(">"),
    LESS_THAN("<"),
    EQUALS("=="),
    GREATER_THAN_EQUAL(">="),
    LESS_THAN_EQUAL("<="),
    NOT_EQUAL("!="),
    AND("&&"),
    OR("||"),
}

fun getOperator(s: String): Operator? {
    try {
        return Operator.values().filter { it.string.equals(s) }[0]
    } catch (e: IndexOutOfBoundsException) {
        return null
    }
}

class BinaryOperator(val expA: Expression, val operator: Operator, val expB: Expression) : Expression(listOf(expA, expB)) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}
