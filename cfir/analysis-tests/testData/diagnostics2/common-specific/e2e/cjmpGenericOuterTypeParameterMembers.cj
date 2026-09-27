// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 two-stage compile: .workbuddy/tmp/cjmp_probe113/generic_outer; no diagnostics.
// Nominal and extend members substitute common outer type parameters T to specific U/V before matching.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class Box<T> {
    public common init()
    public common func identity(value: T): T
}

public common class Holder<T> {
    public common init() {}
    public common func defaultIdentity(value: T): T { value }
}

common extend<T> Holder<T> {
    public common func extendIdentity(value: T): T { value }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class Box<U> {
    public specific init() {}
    public specific func identity(value: U): U { value }
}

specific extend<V> Holder<V> {
    public specific func extendIdentity(value: V): V { value }
}
