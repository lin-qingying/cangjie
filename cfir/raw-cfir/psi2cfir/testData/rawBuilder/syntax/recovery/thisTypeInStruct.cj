// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：struct 成员函数返回 This（官方 parse_this_type_not_allow；1.0.5 / 1.1.3 的 This 只在 class 成员函数与 extend 块成员函数的返回类型位置可用）。
// 官方 cjc 1.0.5 / 1.1.3：parse_this_type_not_allow。
struct S {
    public func me(): This {
        return this
    }
}
