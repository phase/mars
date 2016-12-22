declare i32 @printInt(i32)

declare i32 @castDownAndPrintInt64(i64)

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

define i32 @inc(i32) {
entry:
  %"(a + 1)" = add i32 %0, 1
  ret i32 %"(a + 1)"
}

define i32 @real_main() {
entry:
  br label %"while.c (i < 11)"

"while.c (i < 11)":                               ; preds = %"while.b (i < 11)", %entry
  %storemerge = phi i32 [ %"inc(i)", %"while.b (i < 11)" ], [ 1, %entry ]
  %"(i < 11)" = icmp slt i32 %storemerge, 11
  br i1 %"(i < 11)", label %"while.b (i < 11)", label %"while.o (i < 11)"

"while.b (i < 11)":                               ; preds = %"while.c (i < 11)"
  %"fac(i)" = call i32 @fac(i32 %storemerge)
  %"printInt(fac(i))" = call i32 @printInt(i32 %"fac(i)")
  %"inc(i)" = call i32 @inc(i32 %storemerge)
  br label %"while.c (i < 11)"

"while.o (i < 11)":                               ; preds = %"while.c (i < 11)"
  ret i32 0
}