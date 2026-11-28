// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3: 相同常量拼写冲突，不同进制拼写的原始签名不同。
package e9_radix
common extend Int64 {
    private func f(x: VArray<Int64, $4>): Int64 { 1 }
}
common extend Int64 {
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func f(x: VArray<Int64, $4>): Int64 { 1 }<!>
}
common extend Int64 {
    private func f(x: VArray<Int64, $0x4>): Int64 { 1 }
}
// cjc 1.1.3 同样保留整数分隔符的原始拼写。
common extend Int64 {
    private func g(x: VArray<Int64, $10>): Int64 { 1 }
}
common extend Int64 {
    private func g(x: VArray<Int64, $1_0>): Int64 { 1 }
}
common extend Int64 {
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func g(x: VArray<Int64, $10>): Int64 { 1 }<!>
}
