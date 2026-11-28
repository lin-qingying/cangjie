// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3: 不同 UTF-8 源码拼写的类型名称保留不同 rawMangleName。
package e9_unicode_type
class Café {}
common extend Int64 {
    private func f(value: Café): Unit {}
}
common extend Int64 {
    private func f(value: Café): Unit {}
}
common extend Int64 {
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func f(value: Café): Unit {}<!>
}
