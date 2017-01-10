import std.io

@extern rand() : Int32 0

main ()
    let i = rand(),
    printInt(i),
    if i == 1
        printInt(1)
    elif i == 2
        printInt(2)
    else
        printInt(3)
    ;
    0