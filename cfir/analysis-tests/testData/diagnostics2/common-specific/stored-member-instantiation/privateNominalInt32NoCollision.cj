// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: psi/build/cjmp-verification/20261113-c3-generic-member-identity-probes/private-nominal-int32/result.json
// 同一独立 nominal private 声明集，Int32 与固定 Int64 参数保持不同。

// MODULE: main
// CJMP_MODE: NONE
// MAIN_MODULE
// FILE: source.cj
package c3_member_identity

public class PrivateGeneric<T> {
    private func select(value: T): Unit { }
    private func select(value: Int64): Unit { }
}

public func instantiate(value: PrivateGeneric<Int32>): Unit { }
