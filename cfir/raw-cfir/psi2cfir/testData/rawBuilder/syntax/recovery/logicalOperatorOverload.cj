// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：重载逻辑运算符 `&&` / `||`（官方可重载操作符集合不含它们，报 parse_invalid_overloaded_operator "cannot overload operator '&&'"；
// 一元 `!` 可重载见 expressions/subscriptOperatorForms.cj 与 extendStructForms.cj）。
// 官方 cjc 1.0.5 / 1.1.3：parse_invalid_overloaded_operator。
class L {
    public init() {}

    public operator func && (o: L): L {
        return o
    }

    public operator func || (o: L): L {
        return o
    }
}
