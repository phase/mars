declare i32 @printf(i8*, ...)

@number_format = private unnamed_addr constant [4 x i8] c"%d\0A\00", align 1

define i32 @printInt(i32 %i) nounwind uwtable alwaysinline optsize {
  %1 = alloca i32, align 4
  store i32 %i, i32* %1, align 4
  %2 = load i32, i32* %1, align 4
  %3 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @number_format, i32 0, i32 0), i32 %2)
  ret i32 %3
}

declare i32 @real_main()

define i32 @main() {
  %1 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  %2 = call i32 @real_main()
  ret i32 %2
}

