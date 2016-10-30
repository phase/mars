package xyz.jadonfowler.compiler.ast

interface Type

val T_UNDEF: Type = object : Type {
    override fun toString(): String = "undefined type"
}

val T_INT = object : Type {
    override fun toString(): String = "int"
}

val T_BOOL = object : Type {
    override fun toString(): String = "bool"
}

val T_CHAR = object : Type {
    override fun toString(): String = "char"
}

val T_STRING = object : Type {
    override fun toString(): String = "string"
}

fun getType(name: String): Type {
    return when (name) {
        "int" -> T_INT
        "bool" -> T_BOOL
        "char" -> T_CHAR
        "string" -> T_STRING
        else -> T_UNDEF
    }
}