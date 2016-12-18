package xyz.jadonfowler.compiler.ast

import xyz.jadonfowler.compiler.globalModules
import xyz.jadonfowler.compiler.visitor.Visitor

interface Node

/**
 * Used in ContextVisitor for areas where we need to return something.
 */
class EmptyNode : Node

/**
 * Modules are compilation units that contain global variables, functions, and classes.
 */
class Module(val name: String, val imports: List<Import>, val globalVariables: List<Variable>, val globalFunctions: List<Function>, val globalClasses: List<Clazz>) : Node {

    val errors: MutableList<String> = mutableListOf()

    fun containsReference(reference: Reference): Boolean {
        return globalVariables.map { it.name }.contains(reference.name)
                || globalFunctions.map { it.name }.contains(reference.name)
                || globalClasses.map { it.name }.contains(reference.name)
    }

    fun getNodeFromReference(reference: Reference, localVariables: MutableMap<String, Variable>?): Node? {
        val name = reference.name
        val thingsWithName: MutableList<Node> = mutableListOf()

        thingsWithName.addAll(globalClasses.filter { it.name == name })
        thingsWithName.addAll(globalFunctions.filter { it.name == name })
        thingsWithName.addAll(globalVariables.filter { it.name == name })
        localVariables?.forEach { if (it.key == name) thingsWithName.add(it.value) }

        if (thingsWithName.isNotEmpty()) {
            val lastThingWithName = thingsWithName.last()
            when (lastThingWithName) {
                is Clazz -> return lastThingWithName
                is Function -> return lastThingWithName
                is Variable -> return lastThingWithName
                is Formal -> return lastThingWithName
            }
        }

        // Go through imports
        val imports = imports.map { it.reference.name }
        globalModules.filter { imports.contains(it.name) }.forEach {
            val node = it.getNodeFromReference(reference, null)
            if (node != null) return node
        }
        return null
    }

}

/**
 * Container for name of module to import
 */
class Import(val reference: Reference) : Node

/**
 * Attributes can be put on various declarations
 */
class Attribute(val name: String) : Node

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
class Function(val attributes: List<Attribute>, var returnType: Type, val name: String, val formals: List<Formal>, val statements: List<Statement>, var expression: Expression? = null) : Global, Type {
    fun accept(visitor: Visitor) = visitor.visit(this)

    override fun toString(): String {
        val formals = formals.joinToString(separator = " -> ") { it.type.toString() }
        return "($formals -> $returnType)"
    }
}

/**
 * Formals are the arguments for Functions.
 */
class Formal(type: Type, name: String) : Variable(type, name, null, true) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
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
open class Variable(var type: Type, val name: String, var initialExpression: Expression? = null, val constant: Boolean = false) : Global {
    open fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * Classes (Clazz because Java contains a class named Class) are normal OOP classes, and can contain fields and methods.
 *
 * TODO: Class Constructors
 */
class Clazz(val name: String, val fields: List<Variable>, val methods: List<Function>) : Global, Type {
    fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString(): String = "$name(${fields.map { it.type }.joinToString()})"
}

/**
 * A Reference to a Global declaration.
 */
class Reference(val name: String, var global: Global? = null)

/**
 * Stores the references and arguments for calling a Function.
 */
class FunctionCall(val functionReference: Reference, val arguments: List<Expression> = listOf()) : Node {
    override fun toString(): String =
            functionReference.name + "(" + arguments.map { it.toString() }.joinToString() + ")"
}

/**
 * Stores the references and arguments for a calling a Method.
 */
class MethodCall(val variableReference: Reference, val methodReference: Reference, val arguments: List<Expression> = listOf()) : Node

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
open class CheckedBlock(var exp: Expression, statements: List<Statement>) : Block(statements)

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
 *
 */
class VariableReassignmentStatement(val reference: Reference, var exp: Expression) : Statement() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * Statement wrapper for FunctionCalls
 */
class FunctionCallStatement(val functionCall: FunctionCall) : Statement() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * Statement wrapper for MethodCalls
 */
class MethodCallStatement(val methodCall: MethodCall) : Statement() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * Set field of a Class
 */
class FieldSetterStatement(val variableReference: Reference, val fieldReference: Reference, val expression: Expression) : Statement() {
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

abstract class BooleanExpression(val value: Boolean) : Expression()

/**
 * TrueExpressions are booleans that are only "true".
 */
class TrueExpression : BooleanExpression(true) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString(): String = "true"
}

/**
 * FalseExpressions are booleans that are only "false".
 */
class FalseExpression : BooleanExpression(false) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString(): String = "false"
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
 * Expression wrapper for FunctionCalls
 */
class FunctionCallExpression(val functionCall: FunctionCall) : Expression(functionCall.arguments) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString(): String = functionCall.toString()
}

/**
 * Expression wrapper for MethodCalls
 */
class MethodCallExpression(val methodCall: MethodCall) : Expression(methodCall.arguments) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/**
 * Get field of a Class
 */
class FieldGetterExpression(val variableReference: Reference, val fieldReference: Reference) : Expression() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString(): String = "${variableReference.name}.${fieldReference.name}"
}

/**
 * Operators are constructs that behave like Functions, but differ syntactically.
 */
enum class Operator(val string: String, val aType: Type = T_UNDEF, val bType: Type = T_UNDEF, val returnType: Type = T_UNDEF) {
    // Maths
    PLUS("+", aType = T_INT, bType = T_INT, returnType = T_INT),
    MINUS("-", aType = T_INT, bType = T_INT, returnType = T_INT),
    MULTIPLY("*", aType = T_INT, bType = T_INT, returnType = T_INT),
    DIVIDE("/", aType = T_INT, bType = T_INT, returnType = T_INT),

    // Comparisons
    GREATER_THAN(">", returnType = T_BOOL),
    LESS_THAN("<", returnType = T_BOOL),
    EQUALS("==", returnType = T_BOOL),
    GREATER_THAN_EQUAL(">=", returnType = T_BOOL),
    LESS_THAN_EQUAL("<=", returnType = T_BOOL),
    NOT_EQUAL("!=", returnType = T_BOOL),
    AND("&&", returnType = T_BOOL),
    OR("||", returnType = T_BOOL);

    override fun toString(): String = string
}

/**
 * Get an Operator through its String representation
 */
fun getOperator(s: String): Operator? {
    try {
        return Operator.values().filter { it.string == s }[0]
    } catch (e: IndexOutOfBoundsException) {
        return null
    }
}

/**
 * BinaryOperators are Expressions that contain two sub-Expressions and an Operator that operates on them.
 */
class BinaryOperator(var expA: Expression, val operator: Operator, var expB: Expression) : Expression(listOf(expA, expB)) {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString(): String = "($expA $operator $expB)"
}
