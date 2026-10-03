// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：`Array<T>(x)` 单实参构造（官方 Sema/TypeCheckBuiltinExpr.cpp ChkSingeArgArrayExpr 的单实参路径只在
// 元素类型实参缺失（`Array([1, 2])`）时走；给了类型实参后 1.0.5 / 1.1.3 走重载解析并报 sema_wrong_number_of_arguments
// "missing argument for parameter list '(T, (Int64) -> T)'"，即单实参形态实际对应 `Array<T>(size, init)` 之外的两参重载）。
// 官方 cjc 1.0.5 / 1.1.3：sema_wrong_number_of_arguments + sema_generic_type_without_type_argument。
func singleArg(xs: Array<String>): Unit {
    let a = Array<Int64>([1, 2])
    let b = Array<Int64>(xs)
    let _ = (a, b)
}
