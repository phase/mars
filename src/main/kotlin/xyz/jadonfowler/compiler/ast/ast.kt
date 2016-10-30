package xyz.jadonfowler.compiler.ast

import xyz.jadonfowler.compiler.ast.visitor.Visitor

interface Node

/**
 * Used in ContextVisitor for areas where we need to return something.
 */
class EmptyNode : Node

/**
 * Modules are compilation units that contain global variables, functions, and classes.
 */
class Module(val name: String, val globalVariables: List<Variable>, val globalFunctions: List<Function>, val globalClasses: List<Clazz>) : Node {
}

/**
 * Declaration that can be accessed outside the Module
 */
interface Global : Node

/**
 * Functions have a return type, which is the type of the value that is returned;
 * a list of Formals (aka Arguments), which are used inside the Function;
 * a list of Statements, which are executed in order and can use any formals or global objects;
 * and an optional last expression. The last expression is used as the return value for the function.
 * If there is no last expression, the function returns "void" (aka nothing).
 */
class Function(var returnType: Type, val name: String, val formals: List<Formal>, val statements: List<Statement>, val expression: Expression? = null) : Global, Type {
    fun accept(visitor: Visitor) = visitor.visit(this)

    /**
     * XXX: Returns String of Type
     */
    override fun toString(): String {
        val formals = formals.joinToString(separator = " -> ") { it.type.toString() }
        return "($formals -> ${returnType.toString()})"
    }
}

/**
 * Formals are the arguments for Functions.
 */
class Formal(val type: Type, val name: String) : Node {
    fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * Variables have a type, name, and can have an initial expression.
 *
 * ```
 * let a = 7
 * ```
 *
 * The type will be inferred to 'int' and the initial expression will be an IntegerLiteral(7).
 *
 * If 'constant' is true, the value of this variable can't be changed.
 */
class Variable(var type: Type, val name: String, val initialExpression: Expression? = null, val constant: Boolean = false) : Global {
    fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * Classes (Clazz because Java contains a class named Class) are normal OOP classes, and can contain fields and methods.
 *
 * TODO: Class Constructors
 */
class Clazz(val name: String, val fields: List<Variable>, val methods: List<Function>) : Global, Type {
    fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * A Reference to a Global declaration.
 */
class Reference(val name: String, var global: Global? = null)

// ---------------------------------------------------------------------------------------------------------------------
// STATEMENTS
// ---------------------------------------------------------------------------------------------------------------------

/**
 * Statements are commands that Functions can run to do certain actions. These actions consist of function calls,
 * variable declarations, control flow, etc.
 */
abstract class Statement : Node {
    open fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * Blocks are Statements that contain Statements than can be run.S
 */
open class Block(val statements: List<Statement>) : Statement() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * CheckedBlocks are Blocks with an expression to be evaluated before the Statements run.
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

/**
 * WhileStatements are Blocks that will run over and over as long as their expression is true.
 */
class WhileStatement(exp: Expression, statements: List<Statement>) : CheckedBlock(exp, statements) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * VariableDeclarationStatements add a Variable to the local variable pool that other Statements can access.
 */
class VariableDeclarationStatement(val variable: Variable) : Statement() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * FunctionCallStatements call other Functions with the supplied Expressions.
 */
class FunctionCallStatement(val functionReference: Reference, val arguments: List<Expression> = listOf()) : Statement() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

// ---------------------------------------------------------------------------------------------------------------------
// EXPRESSIONS
// ---------------------------------------------------------------------------------------------------------------------

/**
 * Expressions evaluate to a value.
 */
abstract class Expression(val child: List<Expression> = listOf()) : Node {
    open fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * TrueExpressions are booleans that are only "true".
 */
class TrueExpression : Expression() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * FalseExpressions are booleans that are only "false".
 */
class FalseExpression : Expression() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * IntegerLiterals are an Expression wrapper for Ints.
 */
class IntegerLiteral(val value: Int) : Expression() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString(): String = value.toString()
}

/**
 * StringLiterals are an Expression wrapper for Strings.
 */
class StringLiteral(val value: String) : Expression() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString(): String = value
}

/**
 * Expression wrapper for References
 */
class ReferenceExpression(val reference: Reference) : Expression() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString(): String = reference.name
}

/**
 * FunctionCallStatement as an Expression
 */
class FunctionCallExpression(val functionReference: Reference, val arguments: List<Expression> = listOf()) : Expression(arguments) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * Operators are constructs that behave like Functions, but differ syntactically.
 */
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
    OR("||");

    override fun toString(): String = string
}

/**
 * Get an Operator through its String representation
 */
fun getOperator(s: String): Operator? {
    try {
        return Operator.values().filter { it.string.equals(s) }[0]
    } catch (e: IndexOutOfBoundsException) {
        return null
    }
}

/**
 * BinaryOperators are Expressions that contain two sub-Expressions and an Operator that operates on them.
 */
class BinaryOperator(val expA: Expression, val operator: Operator, val expB: Expression) : Expression(listOf(expA, expB)) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString(): String = "($expA $operator $expB)"
}
