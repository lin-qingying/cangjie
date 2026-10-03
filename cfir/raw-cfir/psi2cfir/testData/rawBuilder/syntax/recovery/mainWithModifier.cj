// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：main 上加可见性修饰符（官方 parse_illegal_modifier_in_scope "unexpected modifier 'public' on main function"）。
// 官方 cjc 1.0.5 / 1.1.3：parse_illegal_modifier_in_scope。
public main(): Int64 {
    return 0
}
