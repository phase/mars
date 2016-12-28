declare void @Accumulator_add(%Accumulator*, i32)

declare void @Accumulator_sub(%Accumulator*, i32)

declare void @Accumulator_mul(%Accumulator*, i32)

declare void @Accumulator_div(%Accumulator*, i32)

define i32 @real_main() {
entry:
  %"malloc(4)" = call i8* @malloc(i64 4)
  %castToAccumulator = bitcast i8* %"malloc(4)" to %Accumulator*
  %INITIAL_ACCUMULATOR_VALUE = load i32, i32* @INITIAL_ACCUMULATOR_VALUE, align 4
  %"a.a = INITIAL_ACCUMULATOR_VALUE" = bitcast i8* %"malloc(4)" to i32*
  store i32 %INITIAL_ACCUMULATOR_VALUE, i32* %"a.a = INITIAL_ACCUMULATOR_VALUE", align 4
  call void @Accumulator_add(%Accumulator* %castToAccumulator, i32 1)
  call void @Accumulator_mul(%Accumulator* %castToAccumulator, i32 7)
  %a.a = load i32, i32* %"a.a = INITIAL_ACCUMULATOR_VALUE", align 4
  %"printInt(a.a)" = call i32 @printInt(i32 %a.a)
  call void @free(i8* %"malloc(4)")
  ret i32 0
}