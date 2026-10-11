// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：This 用在形参类型、字段类型、extend 内置类型、类型别名上（官方 parse_this_type_not_allow "'This' type is not allowed" /
// "'This' type can only be used as the return type of an instance function"）。合法写法见 expressions/thisTypeReturns.cj。
// 官方 cjc 1.0.5 / 1.1.3：parse_this_type_not_allow。
class Param {
    public init() {}

    public func f(o: This): This {
        return o
    }
}
