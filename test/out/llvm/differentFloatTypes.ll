define double @addFloat32To64(float, double) {
entry:
  %"a extended" = fpext float %0 to double
  %"(a +. b)" = fadd double %"a extended", %1
  ret double %"(a +. b)"
}

define fp128 @addFloat64To128(double, fp128) {
entry:
  %"a extended" = fpext double %0 to fp128
  %"(a +. b)" = fadd fp128 %"a extended", %1
  ret fp128 %"(a +. b)"
}

define fp128 @addFloat32To128(float, fp128) {
entry:
  %"a extended" = fpext float %0 to fp128
  %"(a +. b)" = fadd fp128 %"a extended", %1
  ret fp128 %"(a +. b)"
}

define i32 @real_main() {
entry:
  %"sqrtF32(3.0)" = call float @sqrtF32(float 3.000000e+00)
  %"printFloat32(sqrtF32(3.0))" = call i32 @printFloat32(float %"sqrtF32(3.0)")
  %"sqrtF64(5.0)" = call double @sqrtF64(double 5.000000e+00)
  %"printFloat64(sqrtF64(5.0))" = call i32 @printFloat64(double %"sqrtF64(5.0)")
  %"sqrtF64(99.99)" = call double @sqrtF64(double 9.999000e+01)
  %"printFloat64(sqrtF64(99.99))" = call i32 @printFloat64(double %"sqrtF64(99.99)")
  ret i32 0
}