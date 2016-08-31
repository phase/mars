package xyz.jadonfowler.compiler

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RuleContext
import xyz.jadonfowler.compiler.parser.LangLexer
import xyz.jadonfowler.compiler.parser.LangParser

fun main(args: Array<String>) {
    val stream = ANTLRInputStream("""
    add(a : int, b : int) : int {
        return a + b
    }

    c : int = add(5, 6)
    """)
    val lexer = LangLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = LangParser(tokens)
    val result = parser.program()
    explore(result, 0)
}

fun explore(ctx: RuleContext, indentation: Int) {
    val ruleName = LangParser.ruleNames[ctx.ruleIndex]
    for (i in 0..indentation - 1) {
        print("  ")
    }
    println(ruleName + " " + ctx.text.replace('\n', ' '))
    for (i in 0..ctx.childCount - 1) {
        val element = ctx.getChild(i)
        if (element is RuleContext) {
            explore(element, indentation + 1)
        }
    }
}
