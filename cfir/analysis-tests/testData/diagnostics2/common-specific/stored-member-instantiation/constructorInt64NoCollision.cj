// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: psi/build/cjmp-verification/20261113-c3-generic-member-identity-probes/constructor-int64-only/result.json
// 保留固定 Marker 构造器，仅触发 Int64，两个参数类型不同。

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_constructor_candidates_20261026

public common class ConstructorBox<T> {
    public common init(value: T) { }
    public common init(value: Marker) { }
}

public class Marker { }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_constructor_candidates_20261026

public specific class ConstructorBox<U> {
    public specific init(value: U) { }
}

public func instantiateInt64(value: ConstructorBox<Int64>): Unit { }
