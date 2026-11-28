// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：枚举体里写 static 初始化块（官方 ParseEnumBody 不接受 `static init`；报 parse_unexpected_declaration_in_scope "unexpected constructor in enum body"）。
// 合法位置见 declarations/staticInitAndFinalizer.cj。
// 官方 cjc 1.0.5 / 1.1.3：parse_unexpected_declaration_in_scope。
enum StaticInitEnum {
    | A

    static init() {
    }
}
