// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：类体函数上使用 `mut` 修饰符（`mut` 只适用于 prop）。
// 官方 cjc 1.0.5 / 1.1.3：parse_illegal_modifier_in_scope "unexpected modifier 'mut' on function declaration in class body"。
class InvalidMut {
    public mut func bump(): Int64 {
        return 1
    }
}
