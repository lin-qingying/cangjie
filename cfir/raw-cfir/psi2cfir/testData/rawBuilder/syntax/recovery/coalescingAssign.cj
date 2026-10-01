// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：`??=` 复合赋值（官方没有该操作符，只有 `??`）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_expression "expected expression after '', found '='"。
func coalescingAssign(o: ?Int64): Int64 {
    var a = o ?? 0
    a ??= 1
    return a
}
