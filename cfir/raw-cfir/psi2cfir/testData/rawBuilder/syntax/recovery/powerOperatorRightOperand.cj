// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：幂运算右操作数不是 UInt64（`a ** c`，c 是 Int64）。
// 官方 cjc 1.0.5 / 1.1.3：sema_invalid_binary_expr "invalid binary operator '**' on type 'Int64' and 'Int64'"。
func powerOperatorRightOperand(a: Int64, c: Int64): Int64 {
    return a ** c
}
