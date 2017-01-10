import std.io
import test.importClass1

main ()
    let a = new Accumulator(),
    a.a = INITIAL_ACCUMULATOR_VALUE,
    a.add(1),
    a.mul(7),
    printInt(a.a),
    0