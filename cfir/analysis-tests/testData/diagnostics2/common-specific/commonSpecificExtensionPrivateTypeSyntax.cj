// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 signature-syntax.cj：类型列表尾逗号不进入 AST rawMangleName，字面量原拼写和 alias 会保留。
package e9_private_signature_syntax

type Alias = Int64

common extend Int64 {
    private func tuple(value: (Int64, String)): Unit {}
    private func function(value: (Int64) -> Unit): Unit {}
    private func nested(value: Array<Array<Int64>>): Unit {}
    private func hexSize(value: VArray<Int64, $10>): Unit {}
    private func underscoreSize(value: VArray<Int64, $10>): Unit {}
    private func optionalType(value: ?Int64): Unit {}
    private func aliasTarget(value: Int64): Unit {}
}
common extend Int64 {
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func tuple(value: (Int64, String,)): Unit {}<!>
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func function(value: (Int64,) -> Unit): Unit {}<!>
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func nested(value: Array< Array<Int64> >): Unit {}<!>
    private func hexSize(value: VArray<Int64, $0xA>): Unit {}
    private func underscoreSize(value: VArray<Int64, $1_0>): Unit {}
    private func optionalType(value: Option<Int64>): Unit {}
}
common extend Alias {
    private func aliasTarget(value: Int64): Unit {}
}
