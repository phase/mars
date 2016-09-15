package xyz.jadonfowler.compiler

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import xyz.jadonfowler.compiler.ast.ContextVisitor
import xyz.jadonfowler.compiler.ast.visitor.Printer
import xyz.jadonfowler.compiler.parser.LangLexer
import xyz.jadonfowler.compiler.parser.LangParser

fun main(args: Array<String>) {
    val stream = ANTLRInputStream("""
    let c : int = foo(5, 6)
    let d : int = 3 + 2 let e : int = 0 let f : int

    foo (a : int, b : int) : int {
        let g : int = 90128
        if (1 != (2 + 2)) {
            d = b
            if (2 != 14 * 7 - 5) {
                c = b
            } else {
                if (4 >= 2) {
                    c = 7
                }
            }
        }
        printf("%d", a + b)
        return a + b + 1
    }
    """)
    val lexer = LangLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = LangParser(tokens)
    val result = parser.program()
    val contextVisitor = ContextVisitor()
    explore(result, 0)
    val program = contextVisitor.visitProgram(result)
    val printer = Printer()
    printer.visit(program)
}

fun explore(ctx: RuleContext, indentation: Int) {
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
