define i128 @woah(i128, i64) {
entry:
  %"b extended" = zext i64 %1 to i128
  %"(a + b)" = add i128 %"b extended", %0
  ret i128 %"(a + b)"
}

define i64 @add64s(i64, i64) {
entry:
  %"(a + b)" = add i64 %0, %1
  ret i64 %"(a + b)"
}

define i32 @addAndPrint64(i64, i64) {
entry:
  %"add64s(a, b)" = call i64 @add64s(i64 %0, i64 %1)
  %"printInt64(add64s(a, b))" = call i32 @castDownAndPrintInt64(i64 %"add64s(a, b)")
  ret i32 %"printInt64(add64s(a, b))"
}