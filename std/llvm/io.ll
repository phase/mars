declare i32 @printf(i8*, ...)

@int_format = private unnamed_addr constant [4 x i8] c"%d\0A\00", align 1

@f32_format = private unnamed_addr constant [6 x i8] c"%.9g\0A\00", align 1
@f64_format = private unnamed_addr constant [7 x i8] c"%.17g\0A\00", align 1

define i32 @printInt(i32 %i) nounwind uwtable alwaysinline optsize {
    %1 = tail call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @int_format, i64 0, i64 0), i32 %i)
    ret i32 %1
}

define i32 @castDownAndPrintInt64(i64 %i) nounwind uwtable alwaysinline optsize {
    %castDown = trunc i64 %i to i32
    %1 = tail call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @int_format, i64 0, i64 0), i32 %castDown) #0
    ret i32 %1
}

define i32 @printFloat32(float %a) {
    %1 = fpext float %a to double
    %2 = tail call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([6 x i8], [6 x i8]* @f32_format, i64 0, i64 0), double %1)
    ret i32 0
}

define i32 @printFloat64(double %a) {
    %1 = tail call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([7 x i8], [7 x i8]* @f64_format, i64 0, i64 0), double %a)
    ret i32 0
}

; Function Attrs: nounwind uwtable
define i32 @castDownAndPrintFloat128(fp128 %a) #0 {
    %down = fptrunc fp128 %a to double
    %1 = tail call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([7 x i8], [7 x i8]* @f64_format, i64 0, i64 0), double %down)
    ret i32 0
}
