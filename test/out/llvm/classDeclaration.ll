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

define i32 @incrementId(%Account*) {
entry:
  ret i32 0
}