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
import xyz.jadonfowler.compiler.visitor.ContextVisitor
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

    // <Name, Code>
    val modulesToCompile: MutableMap<String, String> = mutableMapOf()

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
            it.errors.forEach(::println)
            failed = true
        }
    }

    if (failed) System.exit(1)

    options.forEach {
        when (it.toLowerCase()) {
            "ast" -> {
                // Print the AST for each module
                modules.forEach { println(PrintPass(it).output) }
            }
            "llvm" -> {
                // Output native code through LLVM Backend
                val bin = File("bin")
                if (!bin.exists())
                    bin.mkdirs()
                modules.forEach {
                    LLVMBackend(it).output(File("bin/${it.name}"))
                }
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
    val contextVisitor = ContextVisitor(moduleName)
    if (explore) explore(result, 0)
    return contextVisitor.visitProgram(result)
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
