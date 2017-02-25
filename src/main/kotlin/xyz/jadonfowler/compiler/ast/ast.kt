package xyz.jadonfowler.compiler.ast

import xyz.jadonfowler.compiler.globalModules
import java.util.*

interface Node

/**
 * Used in ASTBuilder for areas where we need to return something.
 */
class EmptyNode : Node

/**
 * Modules are compilation units that contain global variables, functions, and classes.
 */
class Module(val name: String, val imports: List<Import>, val globalVariables: List<Variable>, val globalFunctions: List<Function>, val globalClasses: List<Clazz>) : Node {

    val errors: MutableList<String> = mutableListOf()

//    TODO: I thought this was needed but I never used it.
//    fun containsReference(reference: Reference): Boolean {
//        return globalVariables.map { it.name }.contains(reference.name)
//                || globalFunctions.map { it.name }.contains(reference.name)
//                || globalClasses.map { it.name }.contains(reference.name)
//    }

    fun getFunctionFromReference(reference: Reference): Function? {
        val moduleName = name
        val name = reference.name
        val thingsWithName: List<Function> = globalFunctions.filter { it.name == name }
        if (thingsWithName.isNotEmpty())
            return thingsWithName.last()

        // Go through imports
        val imports = imports.map { it.reference.name }.filter { it != moduleName }
        globalModules.forEach {
            if (imports.contains(it.name)) {
                val function = it.getFunctionFromReference(reference)
                if (function != null)
                    return function
            }
        }

        return null
    }

    fun getClazzFromReference(reference: Reference): Clazz? {
        val moduleName = name
        val name = reference.name
        val thingsWithName: List<Clazz> = globalClasses.filter { it.name == name }
        if (thingsWithName.isNotEmpty())
            return thingsWithName.last()

        // Go through imports
        val imports = imports.map { it.reference.name }.filter { it != moduleName }
        globalModules.forEach {
            if (imports.contains(it.name)) {
                val clazz = it.getClazzFromReference(reference)
                if (clazz != null)
                    return clazz
            }
        }

        return null
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
class Attribute(val name: String, val values: List<String>) : Node {
    override fun toString(): String = "@$name"
    override fun hashCode(): Int = Objects.hash(name, values)
    override fun equals(other: Any?): Boolean =
            other is Attribute && other.name == name && other.values == values && other.hashCode() == hashCode()
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
class Function(val attributes: List<Attribute>, var returnType: Type, var name: String, var formals: List<Formal>,
               val statements: List<Statement>, var expression: Expression? = null) : Global, Type {
    override fun toString(): String {
        val formals = formals.joinToString(separator = " -> ") { it.type.toString() }
        return "($formals -> $returnType)"
    }

    override fun hashCode(): Int = Objects.hash(attributes, returnType, name, formals, statements, expression)

    override fun equals(other: Any?): Boolean =
            other is Function && other.hashCode() == hashCode() && other.attributes == attributes
                && other.returnType == returnType && other.name == name && other.formals == formals

    fun copy() = Function(attributes, returnType, name, formals, statements, expression)
}

/**
 * Formals are the arguments for Functions.
 */
class Formal(type: Type, name: String) : Variable(type, name, null, true)

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
    override fun toString(): String = "${if (constant) "let" else "var"} $name : $type${if (initialExpression != null) " = $initialExpression" else ""}"

    override fun hashCode(): Int = type.hashCode() + name.hashCode() + constant.hashCode()
    override fun equals(other: Any?): Boolean =
            other is Variable && other.name == name && other.type == type && other.constant == constant && other.hashCode() == hashCode()
}

/**
 * Classes (Clazz because Java contains a class named Class) are normal OOP classes, and can contain fields and methods.
 */
class Clazz(val name: String, val fields: List<Variable>, val methods: List<Function>, val constructor: Function?) : Global, Type {
    override fun toString(): String = "$name(${fields.map { it.type }.joinToString()})"
    override fun hashCode(): Int = Objects.hash(name, fields, methods, constructor)
    override fun equals(other: Any?): Boolean =
            other is Clazz && other.hashCode() == hashCode() && other.name == name
                    && other.fields == fields && other.methods == methods && other.constructor == constructor
}

/**
 * A Reference to a declaration.
 */
class Reference(val name: String) {
    override fun toString(): String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = name == other
}

/**
 * Stores the references and arguments for calling a Function.
 */
class FunctionCall(val functionReference: Reference, val arguments: List<Expression> = listOf()) : Node {
    override fun toString(): String =
            functionReference.name + "(" + arguments.map { it.toString() }.joinToString() + ")"

    override fun hashCode(): Int = Objects.hash(functionReference, arguments)
    override fun equals(other: Any?): Boolean =
            other is FunctionCall && other.functionReference == functionReference && other.arguments == arguments
}

/**
 * Stores the references and arguments for a calling a Method.
 */
class MethodCall(val variableReference: Reference, val methodReference: Reference, val arguments: List<Expression> = listOf()) : Node {
    override fun toString(): String =
            variableReference.name + "." + methodReference.name + "(" + arguments.map { it.toString() }.joinToString() + ")"

    override fun hashCode(): Int = Objects.hash(variableReference, methodReference, arguments)
    override fun equals(other: Any?): Boolean =
            other is MethodCall && other.variableReference == variableReference
                    && other.methodReference == methodReference && other.arguments == arguments
}

// ---------------------------------------------------------------------------------------------------------------------
// STATEMENTS
// ---------------------------------------------------------------------------------------------------------------------

/**
 * Statements are commands that Functions can run to do certain actions. These actions consist of function calls,
 * variable declarations, control flow, etc.
 */
abstract class Statement : Node {
    override fun equals(other: Any?): Boolean = other is Statement && other.hashCode() == hashCode()
}

/**
 * Blocks are Statements that contain Statements than can be run.
 */
open class Block(val statements: List<Statement>) : Statement() {
    override fun hashCode(): Int = Objects.hash(statements)
    override fun equals(other: Any?): Boolean =
            other is Block && other.statements == statements && other.hashCode() == hashCode()
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
 * @param expression Expression to test
 * @param statements Statements to be run
 */
open class CheckedBlock(var expression: Expression, statements: List<Statement>) : Block(statements)

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
class IfStatement(expression: Expression, statements: List<Statement>, var elseStatement: IfStatement?) : CheckedBlock(expression, statements)

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
class WhileStatement(expression: Expression, statements: List<Statement>) : CheckedBlock(expression, statements)

/**
 * VariableDeclarationStatements add a Variable to the local variable pool that other Statements can access.
 */
class VariableDeclarationStatement(val variable: Variable) : Statement()

/**
 *
 */
class VariableReassignmentStatement(val reference: Reference, var expression: Expression) : Statement() {
    override fun toString(): String = "$reference = $expression (${reference.hashCode()}, ${expression.hashCode()})"
    override fun hashCode(): Int = Objects.hash(reference, expression)
    override fun equals(other: Any?): Boolean =
            other is VariableReassignmentStatement && other.reference == reference && other.expression == expression
}

/**
 * Statement wrapper for FunctionCalls
 */
class FunctionCallStatement(val functionCall: FunctionCall) : Statement()

/**
 * Statement wrapper for MethodCalls
 */
class MethodCallStatement(val methodCall: MethodCall) : Statement() {
    override fun toString(): String = methodCall.toString()
}

/**
 * Set field of a Class
 */
class FieldSetterStatement(val variable: Expression, val fieldReference: Reference, val expression: Expression) : Statement() {
    override fun toString(): String = "$variable.$fieldReference = $expression"
}

// ---------------------------------------------------------------------------------------------------------------------
// EXPRESSIONS
// ---------------------------------------------------------------------------------------------------------------------

/**
 * Expressions evaluate to a value.
 */
abstract class Expression(val child: List<Expression> = listOf()) : Node

abstract class BooleanExpression(val value: Boolean) : Expression()

/**
 * TrueExpressions are booleans that are only "true".
 */
class TrueExpression : BooleanExpression(true) {
    override fun toString(): String = "true"
}

/**
 * FalseExpressions are booleans that are only "false".
 */
class FalseExpression : BooleanExpression(false) {
    override fun toString(): String = "false"
}

/**
 * IntegerLiterals are an Expression wrapper for Ints.
 */
class IntegerLiteral(val value: Int) : Expression() {
    override fun toString(): String = value.toString()
}

class FloatLiteral(val value: Double, val type: FloatType) : Expression() {
    override fun toString(): String = value.toString()
}

/**
 * StringLiterals are an Expression wrapper for Strings.
 */
class StringLiteral(val value: String) : Expression() {
    override fun toString(): String = value
}

/**
 * Expression wrapper for References
 */
class ReferenceExpression(val reference: Reference) : Expression() {
    override fun toString(): String = reference.name
    override fun hashCode(): Int = reference.hashCode()
    override fun equals(other: Any?): Boolean = reference == other
}

/**
 * Expression wrapper for FunctionCalls
 */
class FunctionCallExpression(val functionCall: FunctionCall) : Expression(functionCall.arguments) {
    override fun toString(): String = functionCall.toString()
}

/**
 * Expression wrapper for MethodCalls
 */
class MethodCallExpression(val methodCall: MethodCall) : Expression(methodCall.arguments) {
    override fun toString(): String = methodCall.toString()
}

/**
 * Get field of a Class
 */
class FieldGetterExpression(val variable: Expression, val fieldReference: Reference) : Expression() {
    override fun toString(): String = "$variable.$fieldReference"
}

class ClazzInitializerExpression(val classReference: Reference, val arguments: List<Expression>) : Expression(arguments) {
    override fun toString(): String =
            "new ${classReference.name}(" + arguments.map { it.toString() }.joinToString() + ")"
}

/**
 * Operators are constructs that behave like Functions, but differ syntactically.
 */
enum class Operator(val string: String, val aType: Type = T_UNDEF, val bType: Type = T_UNDEF, val returnType: Type = T_UNDEF) {
    // Maths
    PLUS_INT("+", aType = T_INT32, bType = T_INT32, returnType = T_INT32),
    MINUS_INT("-", aType = T_INT32, bType = T_INT32, returnType = T_INT32),
    MULTIPLY_INT("*", aType = T_INT32, bType = T_INT32, returnType = T_INT32),
    DIVIDE_INT("/", aType = T_INT32, bType = T_INT32, returnType = T_INT32),

    PLUS_FLOAT("+.", aType = T_FLOAT32, bType = T_FLOAT32, returnType = T_FLOAT32),
    MINUS_FLOAT("-.", aType = T_FLOAT32, bType = T_FLOAT32, returnType = T_FLOAT32),
    MULTIPLY_FLOAT("*.", aType = T_FLOAT32, bType = T_FLOAT32, returnType = T_FLOAT32),
    DIVIDE_FLOAT("/.", aType = T_FLOAT32, bType = T_FLOAT32, returnType = T_FLOAT32),

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
class BinaryOperator(var expressionA: Expression, val operator: Operator, var expressionB: Expression) : Expression(listOf(expressionA, expressionB)) {
    override fun toString(): String = "($expressionA $operator $expressionB)"
    override fun hashCode(): Int = Objects.hash(expressionA, operator, expressionB)
    override fun equals(other: Any?): Boolean {
        return other is BinaryOperator && other.expressionA == expressionA && other.operator == operator && other.expressionB == expressionB
    }
}
