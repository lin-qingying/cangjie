// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: psi/build/cjmp-verification/20261113-c3-generic-member-identity-probes/old-unmatched-extend-dual/result.json
// extend 的 specific 成员 NOT_MATCHED；未匹配身份不能屏蔽与存储泛型成员的晚期撞型。
// native: 'specific' function 'g' can not find 'common' match
// native: generic instantiation 'ExtendBox<Struct-String>' causes ambiguous function 'g'

// MODULE: common
// CJMP_MODE: COMMON
// FILE: extend_common.cj
package cjmp_unmatched_group_probe

public common class ExtendBox<T> {
    public common init()
}

common extend<T> ExtendBox<T> {
    public func g(value: T): Unit { }
    public func g(value: Bool): Unit { }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: extend_specific.cj
package cjmp_unmatched_group_probe

public specific class ExtendBox<U> {
    public specific init() { }
}

specific extend<V> ExtendBox<V> {
    <!NOT_MATCHED!>public specific func g(value: String): Unit { }<!>
}

public func instantiateString(value: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>ExtendBox<String><!>): Unit { }
public func instantiateInt64(value: ExtendBox<Int64>): Unit { }
