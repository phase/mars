genOperators (a, b : Int) : Int
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
