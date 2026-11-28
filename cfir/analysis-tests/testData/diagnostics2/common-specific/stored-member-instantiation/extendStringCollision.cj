// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: psi/build/cjmp-verification/20261113-c3-generic-member-identity-probes/old-extend-dual/result.json
// extend 存储表按参数身份更新 counterpart；String 撞型，同行 Int64 触发不撞型。
// native: generic instantiation 'ExtendOverloadBox<Struct-String>' causes ambiguous function 'g'

// MODULE: common
// CJMP_MODE: COMMON
// FILE: extend_common.cj
package cjmp_extend_overload_20261026

public common class ExtendOverloadBox<T> {
    public common init()
}

common extend<T> ExtendOverloadBox<T> {
    public common func g(value: T): Unit { }
    public common func g(value: String): Unit { }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: extend_specific.cj
package cjmp_extend_overload_20261026

public specific class ExtendOverloadBox<U> {
    public specific init() { }
}

specific extend<V> ExtendOverloadBox<V> {
    public specific func g(value: V): Unit { }
}

public func instantiateString(value: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>ExtendOverloadBox<String><!>): Unit { }
public func instantiateInt64(value: ExtendOverloadBox<Int64>): Unit { }
