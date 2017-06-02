package xyz.jadonfowler.compiler.ast

interface Type

interface NumericType : Type

interface IntType : NumericType {
    fun getBits(): Int
}

interface FloatType : NumericType {
    fun getBits(): Int
}

private fun makeType(name: String): Type {
    return object : Type {
        override fun toString(): String = name
    }
}

private fun makeIntType(bits: Int): IntType {
    return object : IntType {
        override fun toString(): String = "Int$bits"
        override fun getBits() = bits
    }
}

private fun makeFloatType(bits: Int): FloatType {
    return object : FloatType {
        override fun toString(): String = "Float$bits"
        override fun getBits(): Int = bits
    }
}

/**
 * This isn't like JavaScript's "undefined".
 * This type is used internally to tag objects that don't have a type *yet*.
 */
object T_UNDEF : Type {
    override fun toString(): String = "Undefined"
}

/**
 * Void is only used on Functions.
 */
val T_VOID = makeType("Void")

val T_INT8 = makeIntType(8)
val T_INT16 = makeIntType(16)
val T_INT32 = makeIntType(32)
val T_INT64 = makeIntType(64)
val T_INT128 = makeIntType(128)

val T_FLOAT32 = makeFloatType(32)
val T_FLOAT64 = makeFloatType(64)
val T_FLOAT128 = makeFloatType(128)

val T_BOOL = makeType("Bool")

val T_STRING = makeType("String")

fun getType(name: String, classes: List<Clazz>): Type {
    return when (name) {
        "Int8" -> T_INT8
        "Int16" -> T_INT16
        "Int" -> T_INT32
        "Int32" -> T_INT32
        "Int64" -> T_INT64
        "Int128" -> T_INT128
        "Float" -> T_FLOAT32
        "Float32" -> T_FLOAT32
        "Float64" -> T_FLOAT64
        "Float128" -> T_FLOAT128
        "Bool" -> T_BOOL
        "String" -> T_STRING
        "Void" -> T_VOID
        else -> {
            val possibleClasses = classes.filter { it.name == name }
            if (possibleClasses.isNotEmpty())
                possibleClasses.last()
            else
                T_UNDEF
        }
    }
}

fun getBiggestInt(a: IntType, b: IntType): IntType {
    return if (a.getBits() > b.getBits()) a else b
}

fun getBiggestFloat(a: FloatType, b: FloatType): FloatType {
    return if (a.getBits() > b.getBits()) a else b
}
