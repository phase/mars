> lang-kotlin-antlr-compiler

[![Build Status](https://travis-ci.org/phase/lang-kotlin-antlr-compiler.svg?branch=master)](https://travis-ci.org/phase/lang-kotlin-antlr-compiler)

[![codecov](https://codecov.io/gh/phase/lang-kotlin-antlr-compiler/branch/master/graph/badge.svg)](https://codecov.io/gh/phase/lang-kotlin-antlr-compiler)

This is a compiler written in Kotlin using an ANTLR parser. The planned
backends are LLVM, JVM, JavaScript, and anything else I can get my hands
on.

Here's some example syntax:

```rust
llvm (z : Int, y : Int, x : Int, w : Int)
    let v = 42 + x,
    let u = 45 + v * 67 + 124 - (w * 4) / 5,
    5 + u * z * v
```

This will be parsed into the AST and printed out as:

```
// llvm has 2 statements and a return expression.
function llvm (z: Int, y: Int, x: Int, w: Int) -> Int {
    constant v: Int = (42 + x)
    constant u: Int = (((45 + (v * 67)) + 124) - ((w * 4) / 5))
    return (5 + ((u * z) * v))
}
```

(Notice the type inference: variables and return types are inferred and
checked in the TypePass.)

The LLVM backend will produce something like:

```LLVM
define i32 @llvm(i32, i32, i32, i32) {
entry:
  %"(42 + x)" = add i32 %2, 42
  %"(v * 67)" = mul i32 %"(42 + x)", 67
  %"((45 + (v * 67)) + 124)" = add i32 %"(v * 67)", 169
  %"(w * 4)" = shl i32 %3, 2
  %"((w * 4) / 5)" = sdiv i32 %"(w * 4)", 5
  %"(((45 + (v * 67)) + 124) - ((w * 4) / 5))" = sub i32 %"((45 + (v * 67)) + 124)", %"((w * 4) / 5)"
  %"(u * z)" = mul i32 %"(((45 + (v * 67)) + 124) - ((w * 4) / 5))", %0
  %"((u * z) * v)" = mul i32 %"(u * z)", %"(42 + x)"
  %"(5 + ((u * z) * v))" = add i32 %"((u * z) * v)", 5
  ret i32 %"(5 + ((u * z) * v))"
}
```
