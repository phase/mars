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
  %"(434 + (v * 67))" = add i32 %"(v * 67)", 434
  %"(w * 4)" = shl i32 %4, 2
  %"((w * 4) / 5)" = sdiv i32 %"(w * 4)", 5
  %"((434 + (v * 67)) - ((w * 4) / 5))" = sub i32 %"(434 + (v * 67))", %"((w * 4) / 5)"
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
  %"(((v * z) + 24) - 15)" = add i32 %"(v * z)", 9
  %"((((v * z) + 24) - 15) + y)" = add i32 %"(((v * z) + 24) - 15)", %2
  store i32 %"((((v * z) + 24) - 15) + y)", i32* %t, align 4
  br label %"if.o (z < 10)"

"if.o (z < 10)":                                  ; preds = %"if.t true", %"if.t (z < 10)"
  %t8 = phi i32 [ %"((((v - 7) + 8) + 8) - z)", %"if.t true" ], [ %"((((v * z) + 24) - 15) + y)", %"if.t (z < 10)" ]
  %"(222 - v)" = sub i32 222, %"((v * 2) - z)"
  %"(z * x)" = mul i32 %1, %3
  %"((222 - v) + (z * x))" = add i32 %"(222 - v)", %"(z * x)"
  %"(((222 - v) + (z * x)) - w)" = sub i32 %"((222 - v) + (z * x))", %4
  %"(u * z)" = mul i32 %"((434 + (v * 67)) - ((w * 4) / 5))", %1
  %"((u * z) * v)" = mul i32 %"(u * z)", %"((v * 2) - z)"
  %"(5 + ((u * z) * v))" = add i32 %"((u * z) * v)", 5
  %"(t * 2)" = shl i32 %t8, 1
  %"((t * 2) * l)" = mul i32 %"(t * 2)", %"(((222 - v) + (z * x)) - w)"
  %"((5 + ((u * z) * v)) + ((t * 2) * l))" = add i32 %"(5 + ((u * z) * v))", %"((t * 2) * l)"
  br label %"while.c (i < a)"

"if.t true":                                      ; preds = %"while.b (i < a)"
  %"(((v - 7) + 8) + 8)" = add i32 %"((v * 2) - z)", 9
  %"((((v - 7) + 8) + 8) - z)" = sub i32 %"(((v - 7) + 8) + 8)", %1
  store i32 %"((((v - 7) + 8) + 8) - z)", i32* %t, align 4
  br label %"if.o (z < 10)"
}
