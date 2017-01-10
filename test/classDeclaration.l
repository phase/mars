import std.io

class Account
    var id : Int32
    var amountOfCoins : Int128

    incrementId() : Int32
        let previousId = id,
        id = id + 1,
        previousId

    isAccount(i) : Bool
        id == i

    setId(i : Int32)
        id = i,
        id
;

newAccount () : Account
    let a = new Account(),
    a.id = 7,
    a

getId (a : Account) : Int32
    a.id

main ()
    let a = newAccount(),
    let previousId = a.incrementId(),
    var i = 0,
    while i < 10
        printInt(a.incrementId()),
        let b = 7,
        a.setId(a.id + 7),
        i = i + 1
    ;
    printInt(previousId),
    printInt(getId(a)),
    0
