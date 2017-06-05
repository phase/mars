import std.io

class P : Copy
    let x : Int = 4
    let y : Int = 7
;

main ()
    let a = new P(),
    let b = a,
    b.y = 5,
    printInt(a.x),
    printInt(a.y),
    printInt(b.x),
    printInt(b.y),
    0
