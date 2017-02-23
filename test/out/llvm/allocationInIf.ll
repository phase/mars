define void @Box_init(%Box*, i32) {
entry:
  %value = getelementptr inbounds %Box, %Box* %0, i64 0, i32 0
  store i32 %1, i32* %value, align 4
  ret void
}

define i32 @allocate_thing(i1) {
entry:
  br i1 %0, label %"if.t b", label %"if.o b"

"if.t b":                                         ; preds = %entry
  %"malloc(4) for Box" = call i8* @malloc(i64 4)
  %castToBox = bitcast i8* %"malloc(4) for Box" to %Box*
  call void @Box_init(%Box* %castToBox, i32 7)
  %b = bitcast i8* %"malloc(4) for Box" to i32*
  %b.value = load i32, i32* %b, align 4
  call void @free(i8* %"malloc(4) for Box")
  br label %"if.o b"

"if.o b":                                         ; preds = %entry, %"if.t b"
  %r.0 = phi i32 [ %b.value, %"if.t b" ], [ 0, %entry ]
  ret i32 %r.0
}

define i32 @real_main() {
entry:
  %"allocate_thing(true)" = call i32 @allocate_thing(i1 true)
  %"printInt(allocate_thing(true))" = call i32 @printInt(i32 %"allocate_thing(true)")
  ret i32 0
}