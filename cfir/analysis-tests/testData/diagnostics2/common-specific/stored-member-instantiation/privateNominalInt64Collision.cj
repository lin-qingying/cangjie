// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: psi/build/cjmp-verification/20261113-c3-generic-member-identity-probes/private-nominal-int64/result.json
// 独立 nominal private 成员补入实例检查副本后，Int64 参数撞型。
// native: generic instantiation 'PrivateGeneric<Int64>' causes ambiguous function 'select'

// MODULE: main
// CJMP_MODE: NONE
// MAIN_MODULE
// FILE: source.cj
package c3_member_identity

public class PrivateGeneric<T> {
    private func select(value: T): Unit { }
    private func select(value: Int64): Unit { }
}

public func instantiate(value: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>PrivateGeneric<Int64><!>): Unit { }
