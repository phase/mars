declare i32 @test1()

declare i32 @test3()

define i32 @test2(i32) {
entry:
  %"test3()" = call i32 @test3()
  %"(test3() + a)" = add i32 %"test3()", %0
  ret i32 %"(test3() + a)"
}

define i32 @test4() {
entry:
  ret i32 9
}