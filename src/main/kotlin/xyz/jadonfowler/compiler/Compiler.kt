package xyz.jadonfowler.compiler

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import xyz.jadonfowler.compiler.ast.ContextVisitor
import xyz.jadonfowler.compiler.ast.Module
import xyz.jadonfowler.compiler.ast.pass.TypePass
import xyz.jadonfowler.compiler.ast.visitor.Printer
import xyz.jadonfowler.compiler.parser.LangLexer
import xyz.jadonfowler.compiler.parser.LangParser

fun main(args: Array<String>) {
    // "other file"
    compileString("""
    thing(a : int, b : int) 0
    """)

    val program = compileString("""
    let c = foo(5, 6)
    let d = 3 + 2 let e = 0 let f : int
    let h = 6 let i : int = 7 let j : int = 8
    #let wrong_type : bool = 7
    let str = "test"

    foo (a : int, b : int)
        let g : int = 90128,
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
        return a + b + 1

    class Object

        let field : int = 0

        method (arg : int) 0
            #let local : int = arg + 7,
            #let thing : int = local * 5,
            #local / thing
    ;

    let variable_defined_after_class : int = 0
    """)

    // Go through passes
    TypePass().visit(program)
    Printer().visit(program)
}

fun compileString(s: String): Module {
    val stream = ANTLRInputStream(s)
    val lexer = LangLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = LangParser(tokens)
    val result = parser.program()
    val contextVisitor = ContextVisitor()
    explore(result, 0)
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
