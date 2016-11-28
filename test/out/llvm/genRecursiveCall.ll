define i32 @fac(i32) {
entry:
  %"(n <= 1)" = icmp slt i32 %0, 2
  br i1 %"(n <= 1)", label %"if.o (n <= 1)", label %"if.t true"

"if.o (n <= 1)":                                  ; preds = %entry, %"if.t true"
  %r.0 = phi i32 [ %"(n * fac((n - 1)))", %"if.t true" ], [ 1, %entry ]
  ret i32 %r.0

"if.t true":                                      ; preds = %entry
  %"(n - 1)" = add i32 %0, -1
  %"fac((n - 1))" = call i32 @fac(i32 %"(n - 1)")
  %"(n * fac((n - 1)))" = mul i32 %"fac((n - 1))", %0
  br label %"if.o (n <= 1)"
}