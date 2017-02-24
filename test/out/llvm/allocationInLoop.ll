define i32 @real_main() {
entry:
  br label %"while.c true"

"while.c true":                                   ; preds = %"while.c true", %entry
  %"malloc(4) for Box" = call i8* @malloc(i64 4)
  %castToBox = bitcast i8* %"malloc(4) for Box" to %Box*
  call void @Box_init(%Box* %castToBox, i32 7)
  call void @free(i8* %"malloc(4) for Box")
  br label %"while.c true"
}