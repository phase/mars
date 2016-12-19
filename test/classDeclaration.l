import io

class Account
    var id : Int

    incrementId() : Int
        let previousId = id,
        id = id + 1,
        previousId

    isAccount(i)
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
    let previousId1 = a.incrementId(),
    printInt(a.incrementId()),
    printInt(a.incrementId()),
    printInt(previousId1),
    0
