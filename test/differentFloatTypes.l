import fpmath
import io

addFloat32To64 (a : Float32, b : Float64)
    a +. b

addFloat64To128 (a : Float64, b : Float128)
    a +. b

addFloat32To128 (a : Float32, b : Float128)
    a +. b

main ()
    printFloat32(sqrtF32(3.0)),
    printFloat64(sqrtF64(5.0d)),
    printFloat64(sqrtF64(99.99d)),
    0
