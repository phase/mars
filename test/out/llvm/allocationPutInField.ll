define void @A_init(%A*, %B*) {
entry:
  %b = getelementptr inbounds %A, %A* %0, i64 0, i32 0
  store %B* %1, %B** %b, align 8
  ret void
}

define %A* @t() {
entry:
  %"malloc(4) for B" = call i8* @malloc(i64 4)
  %castToB = bitcast i8* %"malloc(4) for B" to %B*
  %v = bitcast i8* %"malloc(4) for B" to i32*
  store i32 7, i32* %v, align 4
  %"malloc(4) for A" = call i8* @malloc(i64 4)
  %castToA = bitcast i8* %"malloc(4) for A" to %A*
  call void @A_init(%A* %castToA, %B* %castToB)
  %0 = bitcast i8* %"malloc(4) for A" to i8**
  store i8* %"malloc(4) for B", i8** %0, align 8
  ret %A* %castToA
}

define i32 @real_main() {
entry:
  %"t()" = call %A* @t()
  %a = getelementptr inbounds %A, %A* %"t()", i64 0, i32 0
  %a.b = load %B*, %B** %a, align 8
  %a.b1 = getelementptr inbounds %B, %B* %a.b, i64 0, i32 0
  %a.b.v = load i32, i32* %a.b1, align 4
  %"printInt(a.b.v)" = call i32 @printInt(i32 %a.b.v)
  %0 = bitcast %A* %"t()" to i8**
  %load.b2 = load i8*, i8** %0, align 8
  call void @free(i8* %load.b2)
  %bitPointerToa = bitcast %A* %"t()" to i8*
  call void @free(i8* %bitPointerToa)
  ret i32 0
}