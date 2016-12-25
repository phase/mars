declare i32 @real_main()

declare void @srand(i32)

declare i64 @time(i64*)

define i32 @main() {
  ; set random seed to current time
  %time = tail call i64 @time(i64* null)
  %seed = trunc i64 %time to i32
  tail call void @srand(i32 %seed)

  ; call main from user program
  %1 = tail call i32 @real_main()
  ret i32 %1
}
