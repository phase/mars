let G0 = 1234 + 4321
let G1 = 1 + 2 - 3 * 4 + 6 / 6
let G2 = 4

genGlobalsInFunctions (z, y, x, w : Int32)
    var v = 42 + x,
    let u = 45 + v * 67 + 124 - (w * 4) / 5,
    v = v * 2 - z,
    var t = 1,
    if z < 10
        t = v * z
    else
        t = v - z
    ;
    let l = 74 * 3 - u + z,
    5 + u * z * v + t * G2 - G0 * G1 + 2 * l

returnGlobal ()
    G0
