define i32 @genIfStatement(i32, i32, i32) {
entry:
  %"(a == 10)" = icmp eq i32 %0, 10
  %. = select i1 %"(a == 10)", i32 %1, i32 %2
  ret i32 %.
}