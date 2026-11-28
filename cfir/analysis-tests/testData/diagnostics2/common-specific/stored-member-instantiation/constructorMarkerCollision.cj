// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: psi/build/cjmp-verification/20261113-c3-generic-member-identity-probes/old-constructor-dual/result.json
// 实例构造器补入实例检查副本后，泛型参数 Marker 与固定 Marker 构造器撞型。
// native: generic instantiation 'ConstructorBox<Class-Marker>' causes ambiguous function 'init'

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

public func instantiateMarker(value: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>ConstructorBox<Marker><!>): Unit { }
public func instantiateInt64(value: ConstructorBox<Int64>): Unit { }
