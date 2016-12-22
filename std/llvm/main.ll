declare i32 @real_main()

define i32 @main() {
  %1 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  %2 = call i32 @real_main()
  ret i32 %2
}
