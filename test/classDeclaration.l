class Account
    var id = 0

    incrementId() : Int
        id = id + 1,
        id
;

newAccount () : Account
    let a = new Account(),
    a.id = 7,
    a

getId (a : Account) : Int
    a.id
