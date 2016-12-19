import io

class Account
    var id : Int

    incrementId() : Int
        let previousId = id,
        id = id + 1,
        previousId

    isAccount(i) : Bool
        id == i
;

newAccount () : Account
    let a = new Account(),
    a.id = 7,
    a

getId (a : Account) : Int
    a.id

main ()
    let a = newAccount(),
    let previousId = a.incrementId(),
    var i = 0,
    while i < 10
        printInt(a.incrementId())
    ;
    printInt(previousId),
    printInt(getId(a)),
    0
