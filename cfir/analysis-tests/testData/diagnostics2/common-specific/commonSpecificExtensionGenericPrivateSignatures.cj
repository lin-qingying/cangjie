// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 e9-private-signatures/generic-targets.cj；不同 generic Ty 不能仅按名称归组。
package e9_private_generic_targets

class Box<T> { }
interface Bound { }

common extend<T> Box<T> {
    private func own(value: T): Int64 { 1 }
    private func fixed(value: Int64): Int64 { value }
}
common extend<U> Box<U> {
    private func own(value: U): Int64 { 1 }
    private func fixed(value: Int64): Int64 { value }
}
common extend<T> Box<T> where T <: Bound {
    private func own(value: T): Int64 { 1 }
    private func fixed(value: Int64): Int64 { value }
}
common extend Box<Int64> {
    private func concrete(): Int64 { 1 }
}
common extend Box<Int64> {
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func concrete(): Int64 { 2 }<!>
}
common extend Box<String> {
    private func concrete(): Int64 { 2 }
}
