import std.io

class Accumulator
    let a = 0

    init (initialValue : Int32)
        a = initialValue
;

class CPU

    let acc = new Accumulator(1)
    let otherAcc = new Accumulator(2)

    za ()
        acc.a = 0

    ia ()
        acc.a = acc.a + 1

    ga ()
        acc.a

;

main ()
    let cpu = new CPU(),
    cpu.ia(),
    cpu.ia(),
    cpu.acc.a = 7 - cpu.acc.a,
    printInt(cpu.ga()),
    0
