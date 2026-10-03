// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：接口里写 `init` 块（官方 ParseInterfaceBody 报 parse_unexpected_declaration_in_scope "unexpected constructor in interface body"）。
// 官方 cjc 1.0.5 / 1.1.3：parse_unexpected_declaration_in_scope。
interface WithInit {
    init() { }
}

class C <: WithInit {
    public init() {}
}
