define i32 @real_main() {
entry:
  %"malloc(8) for P" = call i8* @malloc(i64 8)
  %x = bitcast i8* %"malloc(8) for P" to i32*
  store i32 4, i32* %x, align 4
  %y = getelementptr inbounds i8, i8* %"malloc(8) for P", i64 4
  %0 = bitcast i8* %y to i32*
  store i32 7, i32* %0, align 4
  %"malloc(8) for P1" = call i8* @malloc(i64 8)
  %copying.a.x.p = bitcast i8* %"malloc(8) for P1" to i32*
  %copying.a.x3 = load i32, i32* %x, align 4
  store i32 %copying.a.x3, i32* %copying.a.x.p, align 4
  %copying.a.y.p = getelementptr inbounds i8, i8* %"malloc(8) for P1", i64 4
  %1 = bitcast i8* %copying.a.y.p to i32*
  %copying.a.y4 = load i32, i32* %0, align 4
  store i32 %copying.a.y4, i32* %1, align 4
  store i32 5, i32* %1, align 4
  %a.x = load i32, i32* %x, align 4
  %"printInt(a.x)" = call i32 @printInt(i32 %a.x)
  %a.y = load i32, i32* %0, align 4
  %"printInt(a.y)" = call i32 @printInt(i32 %a.y)
  %b.x = load i32, i32* %copying.a.x.p, align 4
  %"printInt(b.x)" = call i32 @printInt(i32 %b.x)
  %b.y = load i32, i32* %1, align 4
  %"printInt(b.y)" = call i32 @printInt(i32 %b.y)
  call void @free(i8* %"malloc(8) for P")
  ret i32 0
}
