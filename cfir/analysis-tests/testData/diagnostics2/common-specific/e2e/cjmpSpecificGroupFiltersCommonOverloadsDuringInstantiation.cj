// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class GenericGroupBox<T> {
    public common init()
    public common func f(value: T): Unit { }
    public common func f(value: String): Unit { }
}

common extend<T> GenericGroupBox<T> {
    public common func g(value: T): Unit { }
    public common func g(value: String): Unit { }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_p

public specific class GenericGroupBox<U> {
    public specific init() { }
    public specific func f(value: U): Unit { }
}

specific extend<V> GenericGroupBox<V> {
    public specific func g(value: V): Unit { }
}

public func instantiateStringMembers(
    nominal: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>GenericGroupBox<String><!>,
    extension: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>GenericGroupBox<String><!>,
): Unit { }

public func instantiateInt64Members(box: GenericGroupBox<Int64>): Unit { }
