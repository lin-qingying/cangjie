// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 将两个函数的参数编码为同一 rawMangleName。
package e9_raw_mangle_collision

common extend Int64 {
    private func f(x: (Int64, Int64) -> Int64): Int64 { 1 }
    private func 名称(x: Int64): Int64 { 1 }
    private func cFunction(x: CFunc<(Int64, Int64) -> Int64>): Int64 { 1 }
}

common extend Int64 {
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func f(x: (Int64) -> Int64, y: Int64): Int64 { 1 }<!>
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func `名称`(x: Int64): Int64 { 1 }<!>
    private func `名称`(x: String): Int64 { 1 }
    private func `名字`(x: Int64): Int64 { 1 }
    private func cFunction(x: CFunc<(Int64) -> Int64>, y: Int64): Int64 { 1 }
}
