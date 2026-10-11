// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：整数字面量越界的两种形态——十进制超过 Int64 值域、带后缀超过后缀类型值域（`300u8`）。
// 官方 cjc 1.0.5 / 1.1.3：sema_exceed_num_value_range。
func integerOverflow(): Unit {
    let a = 99999999999999999999
    let b = 300u8
    let _ = (a, b)
}
