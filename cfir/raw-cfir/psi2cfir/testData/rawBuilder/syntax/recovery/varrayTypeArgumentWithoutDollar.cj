// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：VArray 的第二个类型实参不写 `$`（`VArray<Int64, N>`；官方要求 `VArray<Int64, $n>` 且 n 必须是整数字面量，官方诊断名
// parse_varray_type_args_mismatch "expected a '$' follows an integer literal as the second"）。
// 官方 cjc 1.0.5 / 1.1.3：parse_varray_type_args_mismatch。
func varrayTypeArgumentWithoutDollar(a: VArray<Int64, N>): Int64 {
    return a[0]
}
