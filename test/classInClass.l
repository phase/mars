import io

class Accumulator
    let a : Int32
;

class CPU

    let acc : Accumulator
    let otherAcc = Accumulator

    za ()
        acc.a = 0

    ia ()
        acc.a = acc.a + 1

    ga ()
        acc.a

    initAcc()
        acc = new Accumulator(),
        otherAcc = new Accumulator()

;

main ()
    let cpu = new CPU(),
    cpu.za(),
    cpu.ia(),
    cpu.ia(),
    printInt(cpu.ga()),
    0
