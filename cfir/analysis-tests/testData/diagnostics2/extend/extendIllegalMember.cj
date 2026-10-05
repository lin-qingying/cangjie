// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（kind parse_unexpected_declaration_in_scope）：
//   `extend Int64 { init() {} }` → error: unexpected constructor in extend body（锚 `init` 关键字 4 个字符）
//   `extend Int64 { var extra: Int64 = 0 }` → error: unexpected variable declaration in extend body（锚 `var` 关键字 3 个字符）
// 本项目：官方 kind 在 CfirDiagnosticsList 里没有同名诊断，语义上对应 EXTEND_ILLEGAL_MEMBER；
//   范围口径（用户裁决 2026-10-05）：锚点按 CFIR 现状——init 段锚 `init` 关键字，var 段锚变量名（语义定位到具体非法成员），比官方关键字窄锚更利于 IDE 定位。
extend Int64 {
    <!EXTEND_ILLEGAL_MEMBER!>init<!>() {}
}

extend Int64 {
    var <!EXTEND_ILLEGAL_MEMBER!>extra<!>: Int64 = 0
}
