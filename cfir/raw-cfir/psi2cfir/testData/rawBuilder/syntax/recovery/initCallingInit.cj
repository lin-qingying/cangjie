// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：构造器里用 `this.init()` 调用另一个构造器（官方 parse_expected_name "expected a member name after '.' in qualified name, found k"；
// 构造器之间的委托只能写 this(...) / super(...)）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_name / parse_expected_expr_or_decl_in。
class C {
    var v: Int64 = 0

    public init() {
        this.v = 1
    }

    public init(x: Int64) {
        this.v = x
        this.init()
    }
}
