import io

class Account
    var id : Int32
    var amountOfCoins : Int128

    incrementId() : Int32
        let previousId = id,
        id = id + 1,
        previousId
;

main ()
    let a = new Account(),
    a.id = 1,
    let b = new Account(),
    b.id = 2,
    let c = new Account(),
    c.id = 3,
    let d = new Account(),
    d.id = 4,
    let e = new Account(),
    e.id = 5,
    let f = new Account(),
    f.id = 6,
    let g = new Account(),
    g.id = 7,
    let ret = a.id + b.id + c.id + d.id + e.id + f.id + g.id,
    printInt(ret),
    0
