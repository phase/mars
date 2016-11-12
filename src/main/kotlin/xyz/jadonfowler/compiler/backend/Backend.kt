package xyz.jadonfowler.compiler.backend

import xyz.jadonfowler.compiler.ast.Module
import xyz.jadonfowler.compiler.visitor.Visitor
import java.io.File

abstract class Backend(module: Module) : Visitor(module) {

    abstract fun output(file: File)

}