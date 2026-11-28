// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：未使用的泛型参数不阻止 private 判重；约束左侧保留原始 UTF-8 长度。
package e9_unicode_constraints
interface I {}
common extend<é> <!EXTEND_GENERIC_USAGE!>Int64<!> where é <: I {
    private func same(): Unit {}
}
common extend<é> <!EXTEND_GENERIC_USAGE!>Int64<!> where é <: I {
    private func same(): Unit {}
}
common extend<é> <!EXTEND_GENERIC_USAGE!>Int64<!> where `é` <: I {
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func same(): Unit {}<!>
}
