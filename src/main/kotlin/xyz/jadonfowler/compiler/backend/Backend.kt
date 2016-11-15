package xyz.jadonfowler.compiler.backend

import xyz.jadonfowler.compiler.ast.Module
import xyz.jadonfowler.compiler.pass.Pass
import java.io.File

abstract class Backend(module: Module) : Pass(module) {
    abstract fun output(file: File?)
}
