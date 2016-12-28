%Accumulator = type { i32 }

@INITIAL_ACCUMULATOR_VALUE = constant i32 1234

declare i8* @malloc(i64)

declare void @free(i8*)

define void @Accumulator_add(%Accumulator*, i32) {
entry:
  %a = getelementptr inbounds %Accumulator, %Accumulator* %0, i64 0, i32 0
  %a1 = load i32, i32* %a, align 4
  %"(a + b)" = add i32 %a1, %1
  store i32 %"(a + b)", i32* %a, align 4
  ret void
}

define void @Accumulator_sub(%Accumulator*, i32) {
entry:
  %a = getelementptr inbounds %Accumulator, %Accumulator* %0, i64 0, i32 0
  %a1 = load i32, i32* %a, align 4
  %"(a - b)" = sub i32 %a1, %1
  store i32 %"(a - b)", i32* %a, align 4
  ret void
}

define void @Accumulator_mul(%Accumulator*, i32) {
entry:
  %a = getelementptr inbounds %Accumulator, %Accumulator* %0, i64 0, i32 0
  %a1 = load i32, i32* %a, align 4
  %"(a * b)" = mul i32 %a1, %1
  store i32 %"(a * b)", i32* %a, align 4
  ret void
}

define void @Accumulator_div(%Accumulator*, i32) {
entry:
  %a = getelementptr inbounds %Accumulator, %Accumulator* %0, i64 0, i32 0
  %a1 = load i32, i32* %a, align 4
  %"(a / b)" = sdiv i32 %a1, %1
  store i32 %"(a / b)", i32* %a, align 4
  ret void
}