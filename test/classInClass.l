import io

class Accumulator
    let a = 0
;

class CPU

    let acc = new Accumulator()
    let otherAcc = new Accumulator()

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
    printInt(cpu.ga()),
    0
