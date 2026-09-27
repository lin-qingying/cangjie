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
}

common extend<T> Holder<T> {
    public common func extendIdentity(value: T): T { value }
}

public common func shared(): Int64

// MODULE: firstMatch
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific class Box<U> {
    public specific init() {}
    public specific func identity(value: U): U { value }
}

specific extend<V> Holder<V> {
    public specific func extendIdentity(value: V): V { value }
}

public specific func shared(): Int64 { 1 }
public specific func shared(): Int64 { 2 }
