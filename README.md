# mars compiler

[![Build Status](https://travis-ci.org/phase/lang-kotlin-antlr-compiler.svg?branch=master)](https://travis-ci.org/phase/lang-kotlin-antlr-compiler)

[![codecov](https://codecov.io/gh/phase/lang-kotlin-antlr-compiler/branch/master/graph/badge.svg)](https://codecov.io/gh/phase/lang-kotlin-antlr-compiler)

This is a compiler written in Kotlin using an ANTLR parser. The language includes: Global variables, Functions, Classes, Methods, Automatic Memory Management, Type Inference, Calling External Functions, Modules, etc.

Here's some example syntax:

```rust
genComplexExpressionsInWhileLoop (a : Int, z : Int, y : Int, x : Int, w : Int)
    var i = 0,
    var sum = 0,
    while i < a
        var v = 42 + x,
        let u = 45 + v * 67 + 124 - (w * 4) / 5,
        v = v * 2 - z,
        var t = 1,
        if z < 10
            t = v * z
        else
            t = v - z
        ;
        let l = 74 * 3 - v + z * x - w,
        i = 5 + u * z * v + t * 2 * l
    ;
    let r = sum * i,
    r
```

This will be parsed into the AST and printed out as:

```javascript
; Module: genComplexExpressionsInWhileLoop

; genComplexExpressionsInWhileLoop has 4 statements and a return expression.
function genComplexExpressionsInWhileLoop (a: Int, z: Int, y: Int, x: Int, w: Int) -> Int {
    ; VariableDeclarationStatement
    variable i: Int = 0
    ; VariableDeclarationStatement
    variable sum: Int = 0
    ; WhileStatement
    while (i < a) {
        ; VariableDeclarationStatement
        variable v: Int = (42 + x)
        ; VariableDeclarationStatement
        constant u: Int = (((45 + (v * 67)) + 124) - ((w * 4) / 5))
        ; VariableReassignmentStatement
        v = ((v * 2) - z)
        ; VariableDeclarationStatement
        variable t: Int = 1
        ; IfStatement
        if (z < 10) {
            ; VariableReassignmentStatement
            t = (v * z)
        }
        else if (true) {
            ; VariableReassignmentStatement
            t = (v - z)
        }
        ; VariableDeclarationStatement
        constant l: Int = ((((74 * 3) - v) + (z * x)) - w)
        ; VariableReassignmentStatement
        i = ((5 + ((u * z) * v)) + ((t * 2) * l))
    }
    ; VariableDeclarationStatement
    constant r: Int = (sum * i)
    return r    
}
```

(Notice the type inference: variables and return types are inferred and
checked in the TypePass.)

The LLVM backend will produce something like:

```LLVM
; ModuleID = 'genComplexExpressionsInWhileLoop'

define i32 @genComplexExpressionsInWhileLoop(i32, i32, i32, i32, i32) {
entry:
  br label %"while.c (i < a)"

"while.c (i < a)":                                ; preds = %"if.o (z < 10)", %entry
  %i.0 = phi i32 [ 0, %entry ], [ %"((5 + ((u * z) * v)) + ((t * 2) * l))", %"if.o (z < 10)" ]
  %"(i < a)" = icmp slt i32 %i.0, %0
  br i1 %"(i < a)", label %"while.b (i < a)", label %"while.o (i < a)"

"while.b (i < a)":                                ; preds = %"while.c (i < a)"
  %v = alloca i32, align 4
  %"(42 + x)" = add i32 %3, 42
  store i32 %"(42 + x)", i32* %v, align 4
  %"(v * 67)" = mul i32 %"(42 + x)", 67
  %"((45 + (v * 67)) + 124)" = add i32 %"(v * 67)", 169
  %"(w * 4)" = shl i32 %4, 2
  %"((w * 4) / 5)" = sdiv i32 %"(w * 4)", 5
  %"(((45 + (v * 67)) + 124) - ((w * 4) / 5))" = sub i32 %"((45 + (v * 67)) + 124)", %"((w * 4) / 5)"
  %"(v * 2)" = shl i32 %"(42 + x)", 1
  %"((v * 2) - z)" = sub i32 %"(v * 2)", %1
  store i32 %"((v * 2) - z)", i32* %v, align 4
  %t = alloca i32, align 4
  store i32 1, i32* %t, align 4
  %"(z < 10)" = icmp slt i32 %1, 10
  br i1 %"(z < 10)", label %"if.t (z < 10)", label %"if.t true"

"while.o (i < a)":                                ; preds = %"while.c (i < a)"
  ret i32 0

"if.t (z < 10)":                                  ; preds = %"while.b (i < a)"
  %"(v * z)" = mul i32 %"((v * 2) - z)", %1
  store i32 %"(v * z)", i32* %t, align 4
  br label %"if.o (z < 10)"

"if.o (z < 10)":                                  ; preds = %"if.t true", %"if.t (z < 10)"
  %t8 = phi i32 [ %"(v - z)", %"if.t true" ], [ %"(v * z)", %"if.t (z < 10)" ]
  %"((74 * 3) - v)" = sub i32 222, %"((v * 2) - z)"
  %"(z * x)" = mul i32 %1, %3
  %"(((74 * 3) - v) + (z * x))" = add i32 %"((74 * 3) - v)", %"(z * x)"
  %"((((74 * 3) - v) + (z * x)) - w)" = sub i32 %"(((74 * 3) - v) + (z * x))", %4
  %"(u * z)" = mul i32 %"(((45 + (v * 67)) + 124) - ((w * 4) / 5))", %1
  %"((u * z) * v)" = mul i32 %"(u * z)", %"((v * 2) - z)"
  %"(5 + ((u * z) * v))" = add i32 %"((u * z) * v)", 5
  %"(t * 2)" = shl i32 %t8, 1
  %"((t * 2) * l)" = mul i32 %"(t * 2)", %"((((74 * 3) - v) + (z * x)) - w)"
  %"((5 + ((u * z) * v)) + ((t * 2) * l))" = add i32 %"(5 + ((u * z) * v))", %"((t * 2) * l)"
  br label %"while.c (i < a)"

"if.t true":                                      ; preds = %"while.b (i < a)"
  %"(v - z)" = sub i32 %"((v * 2) - z)", %1
  store i32 %"(v - z)", i32* %t, align 4
  br label %"if.o (z < 10)"
}
```
