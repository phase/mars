%Account = type { i32, i128 }

declare i8* @malloc(i64)

declare void @free(i8*)

declare i32 @printInt(i32)

declare i32 @castDownAndPrintInt64(i64)

declare i32 @printFloat32(float)

declare i32 @printFloat64(double)

declare i32 @castDownAndPrintFloat128(fp128)

define i32 @Account_incrementId(%Account*) {
entry:
  %id = getelementptr inbounds %Account, %Account* %0, i64 0, i32 0
  %id1 = load i32, i32* %id, align 4
  %"(id + 1)" = add i32 %id1, 1
  store i32 %"(id + 1)", i32* %id, align 4
  ret i32 %id1
}

define i1 @Account_isAccount(%Account*, i32) {
entry:
  %id = getelementptr inbounds %Account, %Account* %0, i64 0, i32 0
  %id1 = load i32, i32* %id, align 4
  %"(id == i)" = icmp eq i32 %id1, %1
  ret i1 %"(id == i)"
}

define %Account* @newAccount() {
entry:
  %"malloc(20)" = call i8* @malloc(i64 20)
  %castToAccount = bitcast i8* %"malloc(20)" to %Account*
  %"a.id = 7" = bitcast i8* %"malloc(20)" to i32*
  store i32 7, i32* %"a.id = 7", align 4
  ret %Account* %castToAccount
}

define i32 @getId(%Account*) {
entry:
  %a = getelementptr inbounds %Account, %Account* %0, i64 0, i32 0
  %a.id = load i32, i32* %a, align 4
  ret i32 %a.id
}

define i32 @real_main() {
entry:
  %"newAccount()" = call %Account* @newAccount()
  %"a.incrementId()" = call i32 @Account_incrementId(%Account* %"newAccount()")
  br label %"while.c (i < 10)"

"while.c (i < 10)":                               ; preds = %"while.b (i < 10)", %entry
  %storemerge = phi i32 [ %"(i + 1)", %"while.b (i < 10)" ], [ 0, %entry ]
  %"(i < 10)" = icmp slt i32 %storemerge, 10
  br i1 %"(i < 10)", label %"while.b (i < 10)", label %"while.o (i < 10)"

"while.b (i < 10)":                               ; preds = %"while.c (i < 10)"
  %"a.incrementId()2" = call i32 @Account_incrementId(%Account* %"newAccount()")
  %"printInt(a.incrementId())" = call i32 @printInt(i32 %"a.incrementId()2")
  %"(i + 1)" = add i32 %storemerge, 1
  br label %"while.c (i < 10)"

"while.o (i < 10)":                               ; preds = %"while.c (i < 10)"
  %"printInt(previousId)" = call i32 @printInt(i32 %"a.incrementId()")
  %"getId(a)" = call i32 @getId(%Account* %"newAccount()")
  %"printInt(getId(a))" = call i32 @printInt(i32 %"getId(a)")
  %bitPointerToa = bitcast %Account* %"newAccount()" to i8*
  call void @free(i8* %bitPointerToa)
  ret i32 0
}