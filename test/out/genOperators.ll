define i32 @genOperators(i32, i32) {
entry:
  %"(a != b)" = icmp eq i32 %0, %1
  %. = select i1 %"(a != b)", i32 12, i32 10
  ret i32 %.
}