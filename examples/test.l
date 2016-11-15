add (a : Int, b : Int)
    a + b

let G0 = 1234 + 4321
let G1 = 1 + 2 - 3 * 4 + 6 / 6
let G2 = 4

genGlobalsInFunctions (z : Int, y : Int, x : Int, w : Int)
    var v = 42 + x,
    let u = 45 + v * 67 + 124 - (w * 4) / 5,
    v = v * 2 - z,
    var t = 1,
    if z < 10
        t = v * z
    else
        t = v - z
    ;
    let l = 74 * 3 - v + z * x - w,
    5 + u * z * v + t * G2 - G0 * G1 + 2 * l

returnGlobal ()
    G0

genComplexExpressionsInWhileLoop (a : Int, z : Int, y : Int, x : Int, w : Int)
    var i = 0,
    var sum = 0,
    while i < a
        var v = 42 + x,
        let u = 45 * 7 - 16 + 11 + v * 67 + 124 - (w * 4) / 5,
        v = v * 2 - z,
        var t = 1,
        if z < 10
            t = v * z + 24 - 15
        else
            t = v - 7 + 8 + 8 - z
        ;
        let l = 74 * 3 - v + z * x - w,
        i = 5 + u * z * v + t * 2 * l
    ;
    let r = sum * i,
    r

genOperators (a : Int, b : Int) : Int
    var r = 0,
    let c : Bool = a != b,
    if c
        r = r + 10
    else
        if a > b
            r = r + 11
        else
            if a <= b
                r = r + 12
            ;
        ;
    ;
    r
