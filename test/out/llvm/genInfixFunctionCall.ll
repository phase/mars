define i32 @add(i32, i32) {
entry:
  %"(a + b)" = add i32 %0, %1
  ret i32 %"(a + b)"
}

define i32 @multiply(i32, i32) {
entry:
  br label %"while.c (i < x)"

"while.c (i < x)":                                ; preds = %"while.b (i < x)", %entry
  %i.0 = phi i32 [ 0, %entry ], [ %"(i + 1)", %"while.b (i < x)" ]
  %mul.0 = phi i32 [ 0, %entry ], [ %"add(mul, y)", %"while.b (i < x)" ]
  %"(i < x)" = icmp slt i32 %i.0, %0
  br i1 %"(i < x)", label %"while.b (i < x)", label %"while.o (i < x)"

"while.b (i < x)":                                ; preds = %"while.c (i < x)"
  %"(i + 1)" = add i32 %i.0, 1
  %"add(mul, y)" = call i32 @add(i32 %mul.0, i32 %1)
  br label %"while.c (i < x)"

"while.o (i < x)":                                ; preds = %"while.c (i < x)"
  ret i32 %mul.0
}