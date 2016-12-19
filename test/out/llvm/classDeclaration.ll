%Account = type { i32 }

declare i8* @malloc(i64)

declare i32 @printInt(i32)

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
  %"malloc(4)" = call i8* @malloc(i64 4)
  %castToAccount = bitcast i8* %"malloc(4)" to %Account*
  %"a.id = 7" = bitcast i8* %"malloc(4)" to i32*
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
  br label %"while.b (i < 10)"

"while.b (i < 10)":                               ; preds = %entry, %"while.b (i < 10)"
  %"a.incrementId()2" = call i32 @Account_incrementId(%Account* %"newAccount()")
  %"printInt(a.incrementId())" = call i32 @printInt(i32 %"a.incrementId()2")
  br label %"while.b (i < 10)"
}