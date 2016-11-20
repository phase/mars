package xyz.jadonfowler.compiler.ast

interface Type

/**
 * This isn't like JavaScript's "undefined".
 * This type is used internally to tag objects that don't have a type *yet*.
 */
val T_UNDEF: Type = object : Type {
    override fun toString(): String = "Undefined"
}

/**
 * Void is only used on Functions.
 */
val T_VOID = object : Type {
    override fun toString(): String = "Void"
}

val T_INT = object : Type {
    override fun toString(): String = "Int"
}

val T_BOOL = object : Type {
    override fun toString(): String = "Bool"
}

val T_STRING = object : Type {
    override fun toString(): String = "String"
}

fun getType(name: String, classes: List<Clazz>): Type {
    return when (name) {
        "Int" -> T_INT
        "Bool" -> T_BOOL
        "String" -> T_STRING
        "Void" -> T_VOID
        else -> {
            val classes = classes.filter { it.name == name }
            if (classes.isNotEmpty())
                classes.last()
            else
                T_UNDEF
        }
    }
}