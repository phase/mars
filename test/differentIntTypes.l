import io

woah (a : Int128, b : Int64) : Int128
    a + b

add64s (a : Int64, b : Int64) : Int64
    a + b

addAndPrint64 (a : Int64, b : Int64) : Int32
    printInt64(add64s(a, b))