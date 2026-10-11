// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：字符串区间 `"a".."b"`（官方 sema_generic_type_argument_not_match_constraint "generics type arguments do not match the constraint of 'String'"：
// String 不是 Countable）。
// 官方 cjc 1.0.5 / 1.1.3：sema_generic_type_argument_not_match_constraint。
func rangeStringOperands(): Unit {
    let a = "a".."b"
    let _ = a
}
