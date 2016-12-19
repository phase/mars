define i128 @woah(i128, i64) {
entry:
  %"b extended" = zext i64 %1 to i128
  %"(a + b)" = add i128 %"b extended", %0
  ret i128 %"(a + b)"
}