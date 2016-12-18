declare i32 @printInt(i32)

define i32 @real_main() {
entry:
  %"printInt(7)" = call i32 @printInt(i32 7)
  ret i32 0
}