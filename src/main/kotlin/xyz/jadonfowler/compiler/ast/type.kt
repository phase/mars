package xyz.jadonfowler.compiler.ast

interface Type

/**
 * This isn't like JavaScript's "undefined".
 * This type is used internally to tag objects that don't have a type *yet*.
 */
val T_UNDEF: Type = object : Type {
    override fun toString(): String = "undefined type"
}

/**
 * Void is only used on Functions.
 */
val T_VOID = object : Type {
    override fun toString(): String = "void"
}

val T_INT = object : Type {
    override fun toString(): String = "int"
}

val T_BOOL = object : Type {
    override fun toString(): String = "bool"
}

val T_STRING = object : Type {
    override fun toString(): String = "string"
}

fun getType(name: String): Type {
    return when (name) {
        "int" -> T_INT
        "bool" -> T_BOOL
        "string" -> T_STRING
        "void" -> T_VOID
        else -> T_UNDEF
    }
}