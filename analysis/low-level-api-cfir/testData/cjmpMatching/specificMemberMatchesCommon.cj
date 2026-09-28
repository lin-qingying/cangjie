// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// FILE: common.cj
package cjmp_ll

public common class Box<T> {
    public common init()
    public common func identity(value: T): T
}

public common class Holder<T> {
    public common init() {}
    public common func defaultIdentity(value: T): T { value }
}

public common enum Choice<T> {
    | Value(T)
}

common extend<T> Holder<T> {
    public common func defaultExtendIdentity(value: T): T { value }
}

common extend<T> Holder<T> {
    public common func extendIdentity(value: T): T
}

// MODULE: specific()()(common)
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific class Box<U> {
    public specific init() {}
    public specific func identity(value: U): U { value }
}

public specific enum Choice<U> {
    | Value(U)
}

specific extend<V> Holder<V> {
    public specific func extendIdentity(value: V): V { value }
}
