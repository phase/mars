package xyz.jadonfowler.compiler.backend

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import xyz.jadonfowler.compiler.ast.*
import xyz.jadonfowler.compiler.ast.Function
import java.io.File

class JVMBackend(module: Module) : Backend(module) {

    val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

    init {
        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER + ACC_FINAL, module.name, null, "java/lang/Object", null)

        val localVariables: MutableMap<String, Any?> = mutableMapOf()

        module.globalVariables.forEach {
            val value = getValue(it.initialExpression, localVariables)
            localVariables.put(it.name, value)
            val fv = cw.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, it.name, getJVMType(it.type), null, value)
            fv.visitEnd()
        }
        val mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
        mv.visitCode()
        mv.visitInsn(RETURN)
        mv.visitEnd()

        module.globalFunctions.forEach { it.accept(this) }
        module.globalClasses.forEach { it.accept(this) }
    }

    override fun output(file: File?) {
        cw.visitEnd()
    }

    fun getValue(expression: Expression?, localVariables: MutableMap<String, Any?>): Any? {
        return if (expression == null) null
        else when (expression) {
            is IntegerLiteral -> expression.value
            is StringLiteral -> expression.value
            is BooleanExpression -> if (expression.value) 1 else 0
            is ReferenceExpression -> localVariables[expression.reference.name]
            else -> null
        }
    }

    override fun visit(function: Function) {
        val mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, function.name, getJVMType(function), null, null)
    }

    override fun visit(clazz: Clazz) {
        // cw.visitInnerClass()
    }

    companion object {

        fun getJVMType(type: Type): String {
            return when (type) {
                T_VOID -> "V"
                xyz.jadonfowler.compiler.ast.T_INT -> "I"
                T_BOOL -> "Z"
                T_STRING -> "Ljava/lang/String;"
                is Function -> {
                    "(" + type.formals.map { getJVMType(it.type) }.joinToString("") + ")" + getJVMType(type.returnType)
                }
                else -> "Ljava/lang/Object;"
            }
        }

    }

}
