define void @CPU_za(%CPU*) {
entry:
  %acc = getelementptr inbounds %CPU, %CPU* %0, i64 0, i32 0
  %acc1 = load %Accumulator.1*, %Accumulator.1** %acc, align 8
  %"acc.a = 0" = getelementptr inbounds %Accumulator.1, %Accumulator.1* %acc1, i64 0, i32 0
  store i32 0, i32* %"acc.a = 0", align 4
  ret void
}

define void @CPU_ia(%CPU*) {
entry:
  %acc = getelementptr inbounds %CPU, %CPU* %0, i64 0, i32 0
  %acc1 = load %Accumulator.1*, %Accumulator.1** %acc, align 8
  %acc4 = getelementptr inbounds %Accumulator.1, %Accumulator.1* %acc1, i64 0, i32 0
  %acc.a = load i32, i32* %acc4, align 4
  %"(acc.a + 1)" = add i32 %acc.a, 1
  store i32 %"(acc.a + 1)", i32* %acc4, align 4
  ret void
}

define i32 @CPU_ga(%CPU*) {
entry:
  %acc = getelementptr inbounds %CPU, %CPU* %0, i64 0, i32 0
  %acc1 = load %Accumulator.1*, %Accumulator.1** %acc, align 8
  %acc2 = getelementptr inbounds %Accumulator.1, %Accumulator.1* %acc1, i64 0, i32 0
  %acc.a = load i32, i32* %acc2, align 4
  ret i32 %acc.a
}

define i32 @real_main() {
entry:
  %"malloc(8) for CPU" = call i8* @malloc(i64 8)
  %castToCPU = bitcast i8* %"malloc(8) for CPU" to %CPU*
  %"malloc(4) for Accumulator" = call i8* @malloc(i64 4)
  %a = bitcast i8* %"malloc(4) for Accumulator" to i32*
  store i32 0, i32* %a, align 4
  %0 = bitcast i8* %"malloc(8) for CPU" to i8**
  store i8* %"malloc(4) for Accumulator", i8** %0, align 8
  %otherAcc = getelementptr inbounds i8, i8* %"malloc(8) for CPU", i64 8
  %"malloc(4) for Accumulator1" = call i8* @malloc(i64 4)
  %a3 = bitcast i8* %"malloc(4) for Accumulator1" to i32*
  store i32 0, i32* %a3, align 4
  %1 = bitcast i8* %otherAcc to i8**
  store i8* %"malloc(4) for Accumulator1", i8** %1, align 8
  call void @CPU_ia(%CPU* %castToCPU)
  call void @CPU_ia(%CPU* %castToCPU)
  %cpu = bitcast i8* %"malloc(8) for CPU" to %Accumulator.1**
  %cpu.acc = load %Accumulator.1*, %Accumulator.1** %cpu, align 8
  %cpu.acc6 = getelementptr inbounds %Accumulator.1, %Accumulator.1* %cpu.acc, i64 0, i32 0
  %cpu.acc.a = load i32, i32* %cpu.acc6, align 4
  %"(7 - cpu.acc.a)" = sub i32 7, %cpu.acc.a
  store i32 %"(7 - cpu.acc.a)", i32* %cpu.acc6, align 4
  %"cpu.ga()" = call i32 @CPU_ga(%CPU* %castToCPU)
  %"printInt(cpu.ga())" = call i32 @printInt(i32 %"cpu.ga()")
  %load.acc9 = load i8*, i8** %0, align 8
  %load.otherAcc10 = load i8*, i8** %1, align 8
  call void @free(i8* %load.acc9)
  call void @free(i8* %load.otherAcc10)
  call void @free(i8* %"malloc(8) for CPU")
  ret i32 0
}