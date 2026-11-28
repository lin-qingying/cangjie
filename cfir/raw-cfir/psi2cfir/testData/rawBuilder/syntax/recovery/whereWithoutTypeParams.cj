// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：where 约束用在非泛型函数上（官方 ParseDecl.cpp ParseFuncGenericConstraints 的 DiagUnexpectedWhere：
// 没有类型形参时 where 是 parse 错误，报 parse_expected_character "expected ';' or '<NL>', found keyword 'where'"；
// 另附约束写成 `T: Bound` 而不是 `T <: Bound` 的形态）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_character。
func concreteWhere(a: Int64): Int64 where Int64 <: ToString {
    return a
}

func colonBound<T>(a: T): T where T: ToString {
    return a
}
