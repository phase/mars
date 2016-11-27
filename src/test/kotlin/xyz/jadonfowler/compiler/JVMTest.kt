package xyz.jadonfowler.compiler

import org.junit.Test
import org.objectweb.asm.ClassReader
import xyz.jadonfowler.compiler.backend.JVMBackend
import xyz.jadonfowler.compiler.pass.ConstantFoldPass
import xyz.jadonfowler.compiler.pass.TypePass
import java.io.File
import kotlin.test.assertEquals

class JVMTest {

    @Test fun genFields() {
        val code = """
        let a = 7
        """
        val module = compileString("genFields", code)
        TypePass(module)
        ConstantFoldPass(module)

        val j = JVMBackend(module)
        j.output(File("/dev/null"))
        val reader = ClassReader(j.cw.toByteArray())

        assertEquals("genFields", reader.className)
    }

}
