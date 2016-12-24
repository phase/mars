define float @sqrtF32(float) {
    %ret = call float @llvm.sqrt.f32(float %0)
    ret float %ret
}

define double @sqrtF64(double) {
    %ret = call double @llvm.sqrt.f64(double %0)
    ret double %ret
}

define fp128 @sqrtF128(fp128) {
    %ret = call fp128 @llvm.sqrt.f128(fp128 %0)
    ret fp128 %ret
}

declare float @llvm.sqrt.f32(float %Val)
declare double @llvm.sqrt.f64(double %Val)
declare fp128 @llvm.sqrt.f128(fp128 %Val)
