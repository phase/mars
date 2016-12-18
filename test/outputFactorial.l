import io

fac (n)
    var r = 0,
    if n <=1
        r = 1
    else
        r = n * fac(n - 1)
    ;
    r

inc (a)
    a + 1

main ()
    var i = 1,
    while (i < 11)
        printInt(fac(i)),
        i = inc(i)
    ;
    0
