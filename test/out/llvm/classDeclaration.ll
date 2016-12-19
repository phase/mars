%Account = type { i32 }

define %Account* @newAccount() {
entry:
  %"new Account()" = alloca %Account, align 8
  %"a.id = 7" = getelementptr inbounds %Account, %Account* %"new Account()", i64 0, i32 0
  store i32 7, i32* %"a.id = 7", align 8
  ret %Account* %"new Account()"
}

define i32 @getId(%Account*) {
entry:
  %a = getelementptr inbounds %Account, %Account* %0, i64 0, i32 0
  %a.id = load i32, i32* %a, align 4
  ret i32 %a.id
}

define i32 @Account_incrementId(%Account*) {
entry:
  %id = getelementptr inbounds %Account, %Account* %0, i64 0, i32 0
  %id1 = load i32, i32* %id, align 4
  %"(id + 1)" = add i32 %id1, 1
  store i32 %"(id + 1)", i32* %id, align 4
  ret i32 %"(id + 1)"
}