// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：构造器形参写 `let a: Int64` / `a`（无类型）（官方 parse_expected_character "expected ':', found ')' / ','"）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_character。
class MemberParams {
    let a: Int64
    var b: Int64

    public init(a, b) {
        this.a = a
        this.b = b
    }
}
