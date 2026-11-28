// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: psi/build/cjmp-verification/20261113-c3-generic-member-identity-probes/old-unmatched-nominal-dual/result.json
// nominal 的 specific 成员 NOT_MATCHED；未匹配身份不能屏蔽与存储泛型成员的晚期撞型。
// native: 'specific' function 'f' can not find 'common' match
// native: generic instantiation 'NominalBox<Struct-String>' causes ambiguous function 'f'

// MODULE: common
// CJMP_MODE: COMMON
// FILE: nominal_common.cj
package cjmp_unmatched_group_probe

public common class NominalBox<T> {
    public common init()
    public func f(value: T): Unit { }
    public func f(value: Bool): Unit { }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: nominal_specific.cj
package cjmp_unmatched_group_probe

public specific class NominalBox<U> {
    public specific init() { }
    <!NOT_MATCHED!>public specific func f(value: String): Unit { }<!>
}

public func instantiateString(value: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>NominalBox<String><!>): Unit { }
public func instantiateInt64(value: NominalBox<Int64>): Unit { }
