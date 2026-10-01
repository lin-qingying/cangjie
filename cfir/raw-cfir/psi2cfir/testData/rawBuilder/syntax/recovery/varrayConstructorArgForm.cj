// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：VArray 构造调用给了两个实参（官方 Sema/TypeCheckBuiltinExpr.cpp ChkVArrayArg：实参个数必须恰好为 1，
// 也就是无名的 lambda（`size -> T`）或具名 `repeat: T`；多写一个 size 实参会报 sema_varray_args_number_mismatch）。
// 官方 cjc 1.0.5 / 1.1.3：sema_varray_args_number_mismatch "'VArray' constructor accepts only one argument"。
func twoArgs(): Unit {
    let a = VArray<Int64, $2>(2, repeat: 0)
    let _ = a
}

// 同文件的第二个场景：无名实参位置放一个非 lambda 表达式（size 类型是 Int64，lambda 形参 `size` 是未使用形参）。
func lambdaArg(): Unit {
    let a = VArray<Int64, $2>({ size => 1 })
    let b = VArray<Int64, $2>(repeat: 1)
    let _ = (a, b)
}
