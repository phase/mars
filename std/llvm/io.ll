declare i32 @printf(i8*, ...)

@int_format = private unnamed_addr constant [4 x i8] c"%d\0A\00", align 1

@f32_format = private unnamed_addr constant [6 x i8] c"%.9g\0A\00", align 1
@f64_format = private unnamed_addr constant [7 x i8] c"%.17g\0A\00", align 1
@f128_format = private unnamed_addr constant [7 x i8] c"%.17g\0A\00", align 1

define i32 @printInt(i32 %i) nounwind uwtable alwaysinline optsize {
    %1 = alloca i32, align 4
    store i32 %i, i32* %1, align 4
    %2 = load i32, i32* %1, align 4
    %3 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @int_format, i32 0, i32 0), i32 %2)
    ret i32 %3
}

define i32 @castDownAndPrintInt64(i64 %i) nounwind uwtable alwaysinline optsize {
    %castDown = trunc i64 %i to i32
    %ret = call i32 (i32) @printInt(i32 %castDown)
    ret i32 %ret
}

define i32 @printFloat32(float %a) {
    %1 = alloca float, align 4
    store float %a, float* %1, align 4
    %2 = load float, float* %1, align 4
    %3 = fpext float %2 to double
    %4 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([6 x i8], [6 x i8]* @f32_format, i32 0, i32 0), double %3)
    ret i32 0
}

define i32 @printFloat64(double %a) {
    %1 = alloca double, align 8
    store double %a, double* %1, align 8
    %2 = load double, double* %1, align 8
    %3 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([7 x i8], [7 x i8]* @f64_format, i32 0, i32 0), double %2)
    ret i32 0
}

; Function Attrs: nounwind uwtable
define i32 @castDownAndPrintFloat128(fp128 %a) #0 {
  %1 = alloca fp128
  store fp128 %a, fp128* %1
  %2 = load fp128, fp128* %1
  %down = fptrunc fp128 %2 to double
  %3 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([7 x i8], [7 x i8]* @f128_format, i32 0, i32 0), double %down)
  ret i32 0
}

