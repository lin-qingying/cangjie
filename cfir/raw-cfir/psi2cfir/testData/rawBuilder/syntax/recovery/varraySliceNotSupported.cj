// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：给 VArray 下标换算出的区间赋给 Array / 继续下标（官方 VArray 的区间类型不是 Array，`a[0..2]` 的结果不能按 Array 下标 / 迭代）。
// 官方 cjc 1.0.5 / 1.1.3：sema_mismatched_types。
func varraySliceNotSupported(a: VArray<Int64, $3>): Int64 {
    let s: Array<Int64> = a[0..2]
    return s[0]
}
