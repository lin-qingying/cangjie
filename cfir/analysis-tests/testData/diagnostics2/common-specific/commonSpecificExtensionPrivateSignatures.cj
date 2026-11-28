// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 e9-private-signatures/signature-matrix.cj；rawMangleName 保留源码注解。
package e9_private_signature_matrix

type Alias = Int64

common extend Int64 {
    private func same(value: Int64): Int64 { value }
    private func returnType(value: Int64): Int64 { value }
    private func inferredReturn(value: Int64) { value }
    private func parameterName(left!: Int64): Int64 { left }
    private func defaultPresence(value!: Int64 = 1): Int64 { value }
    private func genericName<T>(value: T): Int64 { 1 }
    private func staticModifier(value: Int64): Int64 { value }
    private func aliasType(value: Alias): Int64 { value }
    private func parenthesizedType(value: (Int64)): Int64 { value }
    private func functionType(value: (Int64) -> Unit): Int64 { 1 }
    private func whitespace(value: Int64): Int64 { value }
    private prop item: Int64 { get() { 1 } }
    private prop crossKind: Int64 { get() { 1 } }
}

common extend Int64 {
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func same(value: Int64): Int64 { value }<!>
    private func returnType(value: Int64): String { "" }
    private func inferredReturn(value: Int64): Int64 { value }
    private func parameterName(right!: Int64): Int64 { right }
    private func defaultPresence(value!: Int64): Int64 { value }
    private func genericName<U>(value: U): Int64 { 1 }
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private static func staticModifier(value: Int64): Int64 { value }<!>
    private func aliasType(value: Int64): Int64 { value }
    private func parenthesizedType(value: Int64): Int64 { value }
    private func functionType(value: ((Int64) -> Unit)): Int64 { 1 }
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func whitespace(value: /* type */ Int64): /* result */ Int64 { value }<!>
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private prop item: String { get() { "" } }<!>
    private func crossKind(): Int64 { 1 }
}
