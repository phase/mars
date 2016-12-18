%Account = type { i32 }

define %Account* @newAccount() {
entry:
  %"new Account()" = alloca %Account, align 8
  ret %Account* %"new Account()"
}