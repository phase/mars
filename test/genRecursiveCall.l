fac (n : Int)
    var r = 0,
    if n <=1
        r = 1
    else
        r = n * fac(n - 1)
    ;
    r
