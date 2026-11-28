// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 common-first/ordinary-first；common-private 分支不插入已见集合。
package e9_private_order

common extend Int64 {
    <!COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS!>common private func commonFirst(value: Int64): Int64 { value }<!>
    private func ordinaryFirst(value: Int64): Int64 { value }
}
common extend Int64 {
    private func commonFirst(value: Int64): Int64 { value }
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>common private func ordinaryFirst(value: Int64): Int64 { value }<!>
}
