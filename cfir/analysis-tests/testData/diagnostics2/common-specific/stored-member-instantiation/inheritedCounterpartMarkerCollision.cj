// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: psi/build/cjmp-verification/20261113-c3-generic-member-identity-probes/old-inherited-counterpart-dual/result.json
// 继承 Parent 的存储成员后，specific counterpart 与 common 固定 Marker 默认成员撞型。
// native: redundant modifier: 'public'
// native: redundant modifier: 'public'
// native: redundant modifier: 'public'
// native: generic instantiation 'Child<Class-Marker>' causes ambiguous function 'f'

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_inherited_counterpart_20261026

public common interface Parent<T> {
    <!REDUNDANT_MODIFIER_FOR_TARGET!>public<!> common func f(value: T): Unit { }
    <!REDUNDANT_MODIFIER_FOR_TARGET!>public<!> common func f(value: Marker): Unit { }
}

public common class Child<T> <: Parent<T> {
    public common init()
}

public class Marker { }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_inherited_counterpart_20261026

public specific interface Parent<U> {
    <!REDUNDANT_MODIFIER_FOR_TARGET!>public<!> specific func f(value: U): Unit { }
}

public specific class Child<V> <: Parent<V> {
    public specific init() { }
}

public func instantiateMarker(value: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>Child<Marker><!>): Unit { }
public func instantiateInt64(value: Child<Int64>): Unit { }
