// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3: 泛型参数的原始名称参与 private 函数 rawMangleName。
package e9_function_generic
common extend Int64 {
    private func f<T>(x: T): T { x }
}
common extend Int64 {
    private func f<U>(x: U): U { x }
}
common extend Int64 {
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func f<T>(x: T): T { x }<!>
}
