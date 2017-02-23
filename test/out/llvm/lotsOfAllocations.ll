define i32 @Account_incrementId(%Account.1*) {
entry:
  %id = getelementptr inbounds %Account.1, %Account.1* %0, i64 0, i32 0
  %id1 = load i32, i32* %id, align 4
  %"(id + 1)" = add i32 %id1, 1
  store i32 %"(id + 1)", i32* %id, align 4
  ret i32 %id1
}

define i32 @real_main() {
entry:
  %"malloc(20) for Account" = call i8* @malloc(i64 20)
  %"a.id = 1" = bitcast i8* %"malloc(20) for Account" to i32*
  store i32 1, i32* %"a.id = 1", align 4
  %"malloc(20) for Account1" = call i8* @malloc(i64 20)
  %"b.id = 2" = bitcast i8* %"malloc(20) for Account1" to i32*
  store i32 2, i32* %"b.id = 2", align 4
  %"malloc(20) for Account3" = call i8* @malloc(i64 20)
  %"c.id = 3" = bitcast i8* %"malloc(20) for Account3" to i32*
  store i32 3, i32* %"c.id = 3", align 4
  %"malloc(20) for Account5" = call i8* @malloc(i64 20)
  %"d.id = 4" = bitcast i8* %"malloc(20) for Account5" to i32*
  store i32 4, i32* %"d.id = 4", align 4
  %"malloc(20) for Account7" = call i8* @malloc(i64 20)
  %"e.id = 5" = bitcast i8* %"malloc(20) for Account7" to i32*
  store i32 5, i32* %"e.id = 5", align 4
  %"malloc(20) for Account9" = call i8* @malloc(i64 20)
  %"f.id = 6" = bitcast i8* %"malloc(20) for Account9" to i32*
  store i32 6, i32* %"f.id = 6", align 4
  %"malloc(20) for Account11" = call i8* @malloc(i64 20)
  %"g.id = 7" = bitcast i8* %"malloc(20) for Account11" to i32*
  store i32 7, i32* %"g.id = 7", align 4
  %a.id = load i32, i32* %"a.id = 1", align 4
  %b.id = load i32, i32* %"b.id = 2", align 4
  %"(a.id + b.id)" = add i32 %a.id, %b.id
  %c.id = load i32, i32* %"c.id = 3", align 4
  %"((a.id + b.id) + c.id)" = add i32 %"(a.id + b.id)", %c.id
  %d.id = load i32, i32* %"d.id = 4", align 4
  %"(((a.id + b.id) + c.id) + d.id)" = add i32 %"((a.id + b.id) + c.id)", %d.id
  %e.id = load i32, i32* %"e.id = 5", align 4
  %"((((a.id + b.id) + c.id) + d.id) + e.id)" = add i32 %"(((a.id + b.id) + c.id) + d.id)", %e.id
  %f.id = load i32, i32* %"f.id = 6", align 4
  %"(((((a.id + b.id) + c.id) + d.id) + e.id) + f.id)" = add i32 %"((((a.id + b.id) + c.id) + d.id) + e.id)", %f.id
  %"((((((a.id + b.id) + c.id) + d.id) + e.id) + f.id) + g.id)" = add i32 %"(((((a.id + b.id) + c.id) + d.id) + e.id) + f.id)", 7
  %"printInt(ret)" = call i32 @printInt(i32 %"((((((a.id + b.id) + c.id) + d.id) + e.id) + f.id) + g.id)")
  call void @free(i8* %"malloc(20) for Account")
  call void @free(i8* %"malloc(20) for Account1")
  call void @free(i8* %"malloc(20) for Account3")
  call void @free(i8* %"malloc(20) for Account5")
  call void @free(i8* %"malloc(20) for Account7")
  call void @free(i8* %"malloc(20) for Account9")
  call void @free(i8* %"malloc(20) for Account11")
  ret i32 0
}