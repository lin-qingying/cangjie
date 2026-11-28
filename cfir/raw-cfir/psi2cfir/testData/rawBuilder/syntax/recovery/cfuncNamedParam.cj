// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：CFunc 的形参写具名（`x!:`）（官方 parse_expected_right_delimiter unclosed delimiter '('：CFunc 泛型的类型实参里按普通类型参数解析，不消费 `!`；
// 另外 CFunc 不允许具名实参，官方 sema_cfunc_cannot_have_named_args）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_right_delimiter。
func cfuncNamedParam(g: CFunc<(x!: Int64) -> Unit>): Unit {
    let _ = g
}
