// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// 官方 actual: psi/build/cjmp-verification/20261113-c3-generic-member-identity-probes/old-all-nominal-int64/result.json
// nominal 声明域 counterpart 更新后，Int64 与固定 String 参数保持不同。

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_generic_overload_probe_20261026

public common class NominalOverloadBox<T> {
    public common init()
    public common func f(value: T): Unit { }
    public common func f(value: String): Unit { }
}

public common class ExtendOverloadBox<T> {
    public common init()
}

common extend<T> ExtendOverloadBox<T> {
    public common func g(value: T): Unit { }
    public common func g(value: String): Unit { }
}

public common class CombinedOverloadBox<T> {
    public common init()
    public common func f(value: T): Unit { }
    public common func f(value: String): Unit { }
}

common extend<T> CombinedOverloadBox<T> {
    public common func g(value: T): Unit { }
    public common func g(value: String): Unit { }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: nominal_int64.cj
package cjmp_generic_overload_probe_20261026

public specific class NominalOverloadBox<U> {
    public specific init() { }
    public specific func f(value: U): Unit { }
}

public specific class ExtendOverloadBox<U> {
    public specific init() { }
}

specific extend<V> ExtendOverloadBox<V> {
    public specific func g(value: V): Unit { }
}

public specific class CombinedOverloadBox<U> {
    public specific init() { }
    public specific func f(value: U): Unit { }
}

specific extend<V> CombinedOverloadBox<V> {
    public specific func g(value: V): Unit { }
}

public func instantiateNominalInt64(value: NominalOverloadBox<Int64>): Unit { }
