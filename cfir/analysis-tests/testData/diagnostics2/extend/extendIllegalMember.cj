// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（kind parse_unexpected_declaration_in_scope）：
//   `extend Int64 { init() {} }` → error: unexpected constructor in extend body（锚 `init` 关键字 4 个字符）
//   `extend Int64 { var extra: Int64 = 0 }` → error: unexpected variable declaration in extend body（锚 `var` 关键字 3 个字符）
// 本项目：官方 kind 在 CfirDiagnosticsList 里没有同名诊断，语义上对应 EXTEND_ILLEGAL_MEMBER；
//   标记按官方范围（只包住 `init` / `var` 关键字，不含参数与初始化器）；CFIR CfirExtendExtraChecker 统一 reportOn(整条成员声明)，锚点比官方宽（实现缺口）。
extend Int64 {
    <!EXTEND_ILLEGAL_MEMBER!>init<!>() {}
}

extend Int64 {
    <!EXTEND_ILLEGAL_MEMBER!>var<!> extra: Int64 = 0
}
