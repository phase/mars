package xyz.jadonfowler.compiler

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(ParallelSuite::class)
@Suite.SuiteClasses(
        ASTTest::class,
        TypeCheckingTest::class,
        ConstantFoldTest::class,
        JVMTest::class,
        LLVMTest::class,
        SemanticsTest::class
)
class CompilerTestSuite
