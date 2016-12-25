declare i32 @real_main()

declare void @srand(i32)

declare i64 @time(i64*)

define i32 @main() {
  ; set random seed to current time
  %time = tail call i64 @time(i64* null)
  %seed = trunc i64 %time to i32
  tail call void @srand(i32 %seed)

  %1 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  %2 = call i32 @real_main()
  ret i32 %2
}
