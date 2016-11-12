package xyz.jadonfowler.compiler

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import xyz.jadonfowler.compiler.visitor.ContextVisitor
import xyz.jadonfowler.compiler.ast.Module
import xyz.jadonfowler.compiler.backend.LLVMBackend
import xyz.jadonfowler.compiler.pass.TypePass
import xyz.jadonfowler.compiler.pass.PrintPass
import xyz.jadonfowler.compiler.parser.LangLexer
import xyz.jadonfowler.compiler.parser.LangParser
import java.io.File

val globalModules = mutableListOf<Module>()

fun main(args: Array<String>) {
    // "other file"
    compileString("other", """
    thing(a : Int, b : Int) 0
    """)

    val program = compileString("main", """
    let c = foo(5, 6)
    let d = 3 + 2 let e = 0 let f : Int
    let h = 6 let i : Int = 7 let j : Int = 8
    #let wrong_type : Bool = 7
    let str = "test"

    llvm (z : Int, y : Int, x : Int, w : Int)
        let v = 42 + x,
        let u = 45 + v * 67 + 124 - (w * 4) / 5,
        5 + u * z * v

    foo (a : Int, b : Int)
        let g : Int = 90128,
        if 1 != (2 + 2)
            d = b,
            if 2 != 14 * 7 - 5
                c = b
            else
                if 4 >= 2
                    c = 7,
                    if h >= i || h <= j:
                        print(i, j)
                    ;
                ;
            ;
        ;
        thing(a + b, a - b * g),
        a + b + 1


    let functionWrapper = foo

    class Object

        let field : Int = 0

        method (arg : Int) 0
            #let local : int = arg + 7,
            #let thing : int = local * 5,
            #local / thing
    ;

    let variable_defined_after_class : Int = 0
    """)

    // Go through passes
    TypePass(program)
    PrintPass(program)
    LLVMBackend(program).output(File("/dev/null"))
}

fun compileString(moduleName: String, code: String): Module {
    val stream = ANTLRInputStream(code)
    val lexer = LangLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = LangParser(tokens)
    val result = parser.program()
    val contextVisitor = ContextVisitor(moduleName)
    // explore(result, 0)
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

    for (i in 0..ctx.childCount - 1) {
        val element = ctx.getChild(i)
        if (element is RuleContext) {
            explore(element, indentation + (if (ignore) 0 else 1))
        }
    }
}
