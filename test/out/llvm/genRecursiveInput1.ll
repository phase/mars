declare i32 @test2(i32)

declare i32 @test4()

define i32 @test1() {
entry:
  %"test4()" = call i32 @test4()
  %"(test4() + 1)" = add i32 %"test4()", 1
  ret i32 %"(test4() + 1)"
}

define i32 @test3() {
entry:
  %"test1()" = call i32 @test1()
  %"test2(test1())" = call i32 @test2(i32 %"test1()")
  ret i32 %"test2(test1())"
}