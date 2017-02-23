import std.io

class Box
    let value : Int32

    init (v : Int32)
        value = v
;

allocate_thing(b : Bool)
    var r = 0,
    if b
        let b = new Box(7),
        r = b.value
    else
        r = 0
    ;
    r


main ()
    printInt(allocate_thing(true)),
    0
