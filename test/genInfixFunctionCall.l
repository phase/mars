add (a, b : Int32)
    a + b

multiply (x, y : Int32)
    var mul = 0,
    var i = 0,
    while i < x
        i = i + 1,
        mul = mul `add` y
    ;
    mul
