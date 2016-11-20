package xyz.jadonfowler.compiler.backend

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import xyz.jadonfowler.compiler.ast.*
import java.io.File


class JVMBackend(module: Module) : Backend(module) {

    var cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

    init {
        cw.visit(V1_8, ACC_PUBLIC, module.name, null, "java/lang/Object", null)

        val mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
        mv.visitCode()
        module.globalVariables.forEach {
            val fv = cw.visitField(0, it.name, getJVMType(it.type), null, null)
            fv.visitEnd()
        }
        mv.visitInsn(RETURN)
        mv.visitEnd()

        module.globalVariables.forEach { it.accept(this) }
    }

    override fun output(file: File?) {

    }

    companion object {

        fun getJVMType(type: Type): String {
            return when (type) {
                T_VOID -> "V"
                xyz.jadonfowler.compiler.ast.T_INT -> "I"
                T_BOOL -> "Z"
                T_STRING -> "Ljava/lang/String;"
                else -> "Ljava/lang/Object;"
            }
        }

    }

}
