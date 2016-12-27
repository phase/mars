package xyz.jadonfowler.compiler

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import xyz.jadonfowler.compiler.ast.Module
import xyz.jadonfowler.compiler.backend.JVMBackend
import xyz.jadonfowler.compiler.backend.LLVMBackend
import xyz.jadonfowler.compiler.parser.LangLexer
import xyz.jadonfowler.compiler.parser.LangParser
import xyz.jadonfowler.compiler.pass.ConstantFoldPass
import xyz.jadonfowler.compiler.pass.PrintPass
import xyz.jadonfowler.compiler.pass.TypePass
import xyz.jadonfowler.compiler.visitor.ASTBuilder
import java.io.File

val globalModules = mutableListOf<Module>()

fun main(args: Array<String>) {
    val files: MutableList<String> = mutableListOf()
    val options: MutableList<String> = mutableListOf()

    args.forEach {
        if (it.startsWith("--")) // This argument is an option
            options.add(it.substring(2, it.length))
        else // This argument is a file
            files.add(it)
    }

    if (files.isEmpty()) {
        println("No input files found!")
        System.exit(2)
    }

    // <Name, Code>
    val modulesToCompile: MutableMap<String, String> = mutableMapOf()

    val nonStdFiles = files.size

    // Standard Lib
    val stdDirectory = File("std")
    if (stdDirectory.exists() && stdDirectory.isDirectory) {
        val stdFiles = stdDirectory.listFiles()
        stdFiles.forEach { if (it.isFile) files.add(it.canonicalPath) }
    }

    // Read files that we need to compile
    files.forEach {
        val file = File(it)
        if (file.exists() && !file.isDirectory) {
            modulesToCompile.put(file.nameWithoutExtension, file.readLines().joinToString("\n"))
        } else {
            println("Can't find file '$it'.")
            System.exit(1)
        }
    }

    // Go over every module and parse it
    val modules: List<Module> = modulesToCompile.map { compileString(it.key, it.value) }

    var failed = false

    // Default passes
    modules.forEach {
        TypePass(it)
        ConstantFoldPass(it)

        if (it.errors.size > 0) {
            println("Found errors in ${it.name}:")
            it.errors.forEach { println("    $it") }
            failed = true
        }
    }

    if (failed) System.exit(1)

    // The output should be the same for no matter the order of arguments
    options.sort()

    options.forEach {
        when (it.toLowerCase()) {
            "ast" -> {
                // Print the AST for each input module
                (0..nonStdFiles - 1).forEach { println(PrintPass(modules[it]).output) }
            }
            "llvm" -> {
                // Output native code through LLVM Backend
                val bin = File("bin")
                if (!bin.exists())
                    bin.mkdirs()
                modules.forEach {
                    LLVMBackend(it).output(File("bin/${it.name}"))
                }

                // Compile LLVM specific Std Modules
                val runtime = Runtime.getRuntime()

                val stdLlvmDir = File("std/llvm/")
                if (!stdLlvmDir.exists() || !stdLlvmDir.isDirectory)
                    stdLlvmDir.mkdirs()
                val stdLlvmBin = File("bin/llvm")
                if (!stdLlvmBin.exists())
                    stdLlvmBin.mkdirs()

                val stdLlvmFiles = stdLlvmDir.listFiles().map { it.canonicalPath }
                stdLlvmFiles.forEach {
                    val llcProcess = runtime.exec("llc $it -filetype=obj -o bin/llvm/$it.o")
                    llcProcess.waitFor()
                }
                // Link object files together
                val linkCommand = modules.map { "bin/${it.name}.o" }.toMutableList()
                stdLlvmFiles.forEach {
                    linkCommand.add(it)
                }
                linkCommand.add(0, "clang")
                linkCommand.add("-lm")
                linkCommand.add("-o")
                linkCommand.add("bin/exec/${modules[0].name}")

                val execDir = File("bin/exec")
                if (!execDir.exists())
                    execDir.mkdirs()
                val clangProcess = runtime.exec(linkCommand.joinToString(separator = " "))
                println(clangProcess.waitFor())
                println("Compiled Executable " + String(clangProcess.inputStream.readBytes()))
                println(String(clangProcess.errorStream.readBytes()))
            }
            "jvm" -> {
                val bin = File("bin")
                if (!bin.exists())
                    bin.mkdirs()
                modules.forEach {
                    JVMBackend(it).output(File("bin/${it.name}.class"))
                }
            }
        }
    }
}

fun compileString(moduleName: String, code: String, explore: Boolean = false): Module {
    val stream = ANTLRInputStream(code)
    val lexer = LangLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = LangParser(tokens)
    val result = parser.program()
    val astBuilder = ASTBuilder(moduleName)
    if (explore) explore(result, 0)
    return astBuilder.visitProgram(result)
}

fun explore(ctx: RuleContext, indentation: Int = 0) {
    val ignore = ctx.childCount === 1 && ctx.getChild(0) is ParserRuleContext
    if (!ignore) {
        val ruleName = LangParser.ruleNames[ctx.ruleIndex]
        println("    ".repeat(indentation)
                + ctx.javaClass.name.split(".").last()
                + " " + ruleName + ":\n"
                + "    ".repeat(indentation)
                + ctx.text
                + "\n")
    }

    (0..ctx.childCount - 1).forEach {
        val element = ctx.getChild(it)
        if (element is RuleContext)
            explore(element, indentation + (if (ignore) 0 else 1))
    }
}
