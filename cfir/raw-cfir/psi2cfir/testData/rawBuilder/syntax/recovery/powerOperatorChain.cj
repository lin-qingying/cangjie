// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：幂运算链式 / 右操作数不是 UInt64（官方 `**` 的右操作数类型固定为 UInt64、不可链式；合法形态见 expressions/powerOperatorForms.cj）。
// 官方 cjc 1.0.5 / 1.1.3：sema_invalid_binary_expr "invalid binary operator '**' on type 'UInt64' and 'UInt64'"。
func powerOperatorChain(a: Int64, b: UInt64, c: UInt64): Int64 {
    let x = a ** b ** c
    let y = a ** (b ** c)
    let z = a ** c
    return x + y + z
}
