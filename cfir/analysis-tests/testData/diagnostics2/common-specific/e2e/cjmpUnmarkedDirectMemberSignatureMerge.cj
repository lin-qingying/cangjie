// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3: the unmarked specific direct member conflicts with the common declaration; it reports overload conflict at the specific function and no generic-instantiation ambiguity.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class Box<T> {
    public common init()
    public common func f(value: T): Unit { }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class Box<U> {
    public specific init() { }
    public func <!CONFLICTING_OVERLOADS!>f<!>(value: U): Unit { }
}

public func use(box: Box<Int64>): Unit { }
