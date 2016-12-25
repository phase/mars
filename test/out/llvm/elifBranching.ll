define i32 @real_main() {
entry:
  %"rand()" = call i32 @rand()
  %"printInt(i)" = call i32 @printInt(i32 %"rand()")
  switch i32 %"rand()", label %"if.t true" [
    i32 1, label %"if.t (i == 1)"
    i32 2, label %"if.t (i == 2)"
  ]

"if.t (i == 1)":                                  ; preds = %entry
  %"printInt(1)" = call i32 @printInt(i32 1)
  br label %"if.o (i == 1)"

"if.o (i == 1)":                                  ; preds = %"if.t (i == 2)", %"if.t true", %"if.t (i == 1)"
  ret i32 0

"if.t (i == 2)":                                  ; preds = %entry
  %"printInt(2)" = call i32 @printInt(i32 2)
  br label %"if.o (i == 1)"

"if.t true":                                      ; preds = %entry
  %"printInt(3)" = call i32 @printInt(i32 3)
  br label %"if.o (i == 1)"
}