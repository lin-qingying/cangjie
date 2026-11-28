// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：二维数组字面量 `[[...], [...],]`（最后一行后面多写一个逗号）——官方 1.0.5 / 1.1.3 把这样的字面量按"把 `[` 之间的
// 逗号当成一维元素分隔符"处理，推断为一维 Array<Int64>，于是 `a[1][1]` 报 sema_invalid_subscript_expr
// "invalid subscript operator [] on type 'Int64' with index type 'Int64'"；去掉最后一行后的逗号即可得到二维类型
// （见 large/wideArrayLiterals.cj 的 grid）。
// 官方 cjc 1.0.5 / 1.1.3：sema_invalid_subscript_expr。
func flatNestedLiteral(): Int64 {
    let a = [
        [11, 12, 13, 14, 15],
        [21, 22, 23, 24, 25],
    ]
    return a[1][1]
}

