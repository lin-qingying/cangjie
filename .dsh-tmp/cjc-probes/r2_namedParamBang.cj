// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：函数类型 / 元组类型的形参名写成带感叹号的形式 `x!: T`（官方 ParseType.cpp ParseTypeParameterInTupleType 只接受
// `name:`，`!` 不被消费因而报 unclosed delimiter '('；感叹号只属于函数声明的形参 `func f(x!: T)`）。
// 官方 cjc 1.0.5 / 1.1.3：2 条 parse_expected_right_delimiter unclosed delimiter '('。
func namedParamBang(g: (x!: Int64, y!: String) -> Bool): Bool {
    return g(1, "x")
}
