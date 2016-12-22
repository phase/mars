@G0 = constant i32 5555
@G1 = constant i32 -8
@G2 = constant i32 4

declare i8* @malloc(i64)

declare void @free(i8*)

define i32 @genGlobalsInFunctions(i32, i32, i32, i32) {
entry:
  %"(42 + x)" = add i32 %2, 42
  %"(v * 67)" = mul i32 %"(42 + x)", 67
  %"(169 + (v * 67))" = add i32 %"(v * 67)", 169
  %"(w * 4)" = shl i32 %3, 2
  %"((w * 4) / 5)" = sdiv i32 %"(w * 4)", 5
  %"((169 + (v * 67)) - ((w * 4) / 5))" = sub i32 %"(169 + (v * 67))", %"((w * 4) / 5)"
  %"(v * 2)" = shl i32 %"(42 + x)", 1
  %"((v * 2) - z)" = sub i32 %"(v * 2)", %0
  %"(z < 10)" = icmp slt i32 %0, 10
  %"(v * z)" = mul i32 %"((v * 2) - z)", %0
  %"(v - z)" = sub i32 %"((v * 2) - z)", %0
  %t.0 = select i1 %"(z < 10)", i32 %"(v * z)", i32 %"(v - z)"
  %"(222 - v)" = sub i32 222, %"((v * 2) - z)"
  %"(z * x)" = mul i32 %0, %2
  %"((222 - v) + (z * x))" = add i32 %"(222 - v)", %"(z * x)"
  %"(((222 - v) + (z * x)) - w)" = sub i32 %"((222 - v) + (z * x))", %3
  %"(u * z)" = mul i32 %"((169 + (v * 67)) - ((w * 4) / 5))", %0
  %"((u * z) * v)" = mul i32 %"(u * z)", %"((v * 2) - z)"
  %"(5 + ((u * z) * v))" = add i32 %"((u * z) * v)", 5
  %"(t * G2)" = shl i32 %t.0, 2
  %"((5 + ((u * z) * v)) + (t * G2))" = add i32 %"(5 + ((u * z) * v))", %"(t * G2)"
  %"(((5 + ((u * z) * v)) + (t * G2)) - (G0 * G1))" = add i32 %"((5 + ((u * z) * v)) + (t * G2))", 44440
  %"(2 * l)" = shl i32 %"(((222 - v) + (z * x)) - w)", 1
  %"((((5 + ((u * z) * v)) + (t * G2)) - (G0 * G1)) + (2 * l))" = add i32 %"(((5 + ((u * z) * v)) + (t * G2)) - (G0 * G1))", %"(2 * l)"
  ret i32 %"((((5 + ((u * z) * v)) + (t * G2)) - (G0 * G1)) + (2 * l))"
}

define i32 @returnGlobal() {
entry:
  ret i32 5555
}