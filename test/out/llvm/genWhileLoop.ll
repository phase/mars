define i32 @genWhileLoop(i32) {
entry:
  br label %"while.c (i < a)"

"while.c (i < a)":                                ; preds = %"while.b (i < a)", %entry
  %i.0 = phi i32 [ 0, %entry ], [ %"(i + 1)", %"while.b (i < a)" ]
  %storemerge = phi i32 [ %"(sum + a)", %"while.b (i < a)" ], [ 0, %entry ]
  %"(i < a)" = icmp slt i32 %i.0, %0
  br i1 %"(i < a)", label %"while.b (i < a)", label %"while.o (i < a)"

"while.b (i < a)":                                ; preds = %"while.c (i < a)"
  %"(i + 1)" = add i32 %i.0, 1
  %"(sum + a)" = add i32 %storemerge, %0
  br label %"while.c (i < a)"

"while.o (i < a)":                                ; preds = %"while.c (i < a)"
  %"(sum * i)" = mul i32 %storemerge, %i.0
  ret i32 %"(sum * i)"
}