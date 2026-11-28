// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_extend_p

public common class ExtendOnlyBox<T> {
    public common init()
}

common extend<T> ExtendOnlyBox<T> {
    public common func g(value: T): Unit { }
    public common func g(value: String): Unit { }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_extend_p

public specific class ExtendOnlyBox<U> {
    public specific init() { }
}

specific extend<V> ExtendOnlyBox<V> {
    public specific func g(value: V): Unit { }
}

public func instantiateString(value: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>ExtendOnlyBox<String><!>): Unit { }
public func instantiateInt64(value: ExtendOnlyBox<Int64>): Unit { }
