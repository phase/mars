add (a, b : Int)
    a + b

multiply (x, y : Int)
    var mul = 0,
    var i = 0,
    while i < x
        i = i + 1,
        mul = mul `add` y
    ;
    mul
