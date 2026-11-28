// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: psi/build/cjmp-verification/20261113-c3-generic-member-identity-probes/constructor-exact-only/result.json
// 仅有泛型构造器 counterpart；同签名更新不产生额外构造器候选，两个实例均无错误。

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_constructor_candidates_20261026

public common class ConstructorBox<T> {
    public common init(value: T) { }
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

public func instantiateMarker(value: ConstructorBox<Marker>): Unit { }
public func instantiateInt64(value: ConstructorBox<Int64>): Unit { }
