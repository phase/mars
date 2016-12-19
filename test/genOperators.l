genOperators (a, b : Int32) : Int32
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
