define void @CPU_za(%CPU*) {
entry:
  %acc = getelementptr inbounds %CPU, %CPU* %0, i64 0, i32 0
  %acc1 = load %Accumulator*, %Accumulator** %acc, align 8
  %"acc.a = 0" = getelementptr inbounds %Accumulator, %Accumulator* %acc1, i64 0, i32 0
  store i32 0, i32* %"acc.a = 0", align 4
  ret void
}

define void @CPU_ia(%CPU*) {
entry:
  %acc = getelementptr inbounds %CPU, %CPU* %0, i64 0, i32 0
  %acc1 = load %Accumulator*, %Accumulator** %acc, align 8
  %acc4 = getelementptr inbounds %Accumulator, %Accumulator* %acc1, i64 0, i32 0
  %acc.a = load i32, i32* %acc4, align 4
  %"(acc.a + 1)" = add i32 %acc.a, 1
  store i32 %"(acc.a + 1)", i32* %acc4, align 4
  ret void
}

define i32 @CPU_ga(%CPU*) {
entry:
  %acc = getelementptr inbounds %CPU, %CPU* %0, i64 0, i32 0
  %acc1 = load %Accumulator*, %Accumulator** %acc, align 8
  %acc2 = getelementptr inbounds %Accumulator, %Accumulator* %acc1, i64 0, i32 0
  %acc.a = load i32, i32* %acc2, align 4
  ret i32 %acc.a
}

define void @CPU_initAcc(%CPU*) {
entry:
  %"malloc(4) for Accumulator" = call i8* @malloc(i64 4)
  %1 = bitcast %CPU* %0 to i8**
  store i8* %"malloc(4) for Accumulator", i8** %1, align 8
  %"malloc(4) for Accumulator1" = call i8* @malloc(i64 4)
  %otherAcc = getelementptr inbounds %CPU, %CPU* %0, i64 0, i32 1
  %2 = bitcast %Accumulator** %otherAcc to i8**
  store i8* %"malloc(4) for Accumulator1", i8** %2, align 8
  call void @free(i8* %"malloc(4) for Accumulator")
  call void @free(i8* %"malloc(4) for Accumulator1")
  ret void
}

define i32 @real_main() {
entry:
  %"malloc(8) for CPU" = call i8* @malloc(i64 8)
  %castToCPU = bitcast i8* %"malloc(8) for CPU" to %CPU*
  %"malloc(4) for acc" = call i8* @malloc(i64 4)
  %0 = bitcast i8* %"malloc(8) for CPU" to i8**
  store i8* %"malloc(4) for acc", i8** %0, align 8
  %otherAcc = getelementptr inbounds i8, i8* %"malloc(8) for CPU", i64 8
  %"malloc(4) for otherAcc" = call i8* @malloc(i64 4)
  %1 = bitcast i8* %otherAcc to i8**
  store i8* %"malloc(4) for otherAcc", i8** %1, align 8
  call void @CPU_za(%CPU* %castToCPU)
  call void @CPU_ia(%CPU* %castToCPU)
  call void @CPU_ia(%CPU* %castToCPU)
  %"cpu.ga()" = call i32 @CPU_ga(%CPU* %castToCPU)
  %"printInt(cpu.ga())" = call i32 @printInt(i32 %"cpu.ga()")
  %load.acc4 = load i8*, i8** %0, align 8
  %load.otherAcc5 = load i8*, i8** %1, align 8
  call void @free(i8* %load.acc4)
  call void @free(i8* %load.otherAcc5)
  call void @free(i8* %"malloc(8) for CPU")
  ret i32 0
}