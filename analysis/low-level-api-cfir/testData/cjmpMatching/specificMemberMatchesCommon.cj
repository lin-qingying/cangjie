// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll

public common class Box<T> {
    public common init()
    public common init(value: Int64) {}
    public common func identity(value: T): T
    public common func commonOnly(): Int64 { 2 }
}

public common class Holder<T> {
    public common init() {}
    public common func defaultIdentity(value: T): T { value }
}

public common enum Choice<T> {
    | Value(T)
}

public common class Other<T> {}

common extend<T> Holder<T> {
    public common func defaultExtendIdentity(value: T): T { value }
}

common extend<T> Holder<T> {
    public common func extendIdentity(value: T): T
}

common extend<T> Other<T> {
    public common func unrelated(value: T): T { value }
}

// MODULE: specificMemberMatchesCommon()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific class Box<U> {
    public specific init() {}
    public specific func identity(value: U): U { value }
}

public type BoxAlias = Box<Int64>

public specific enum Choice<U> {
    | Value(U)
}

specific extend<V> Holder<V> {
    public specific func extendIdentity(value: V): V { value }
}
