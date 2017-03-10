import std.io

class B
    let v = 7
;

class A
    var b : B

    init (l : B)
        b = l
;

t () : A
    let b = new B(),
    let a = new A(b),
    a.b = b,
    a

main ()
    let a = t(),
    printInt(a.b.v),
    0
