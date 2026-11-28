// LANGUAGE_VERSION: 1.2.0-alpha.20
// CJMP_EXPECTED_COUNTERPART_COUNT: 2
// MODULE: common1
// CJMP_MODE: COMMON
// FILE: common1.cj
package cjmp_ll

public common class Box<T> {
    public common init()
    public common func identity(value: T): T
}

common extend<T> Box<T> {
    public common func extendIdentity(value: T): T { value }
}

public common func select<T>(candidate!: T): T { candidate }

// MODULE: common2
// CJMP_MODE: COMMON
// FILE: common2.cj
package cjmp_ll

public common class Box<T> {
    public common init()
    public common func identity(value: T): T
}

common extend<T> Box<T> {
    public common func extendIdentity(value: T): T { value }
}

public common func select<T>(candidate!: T): T { candidate }

// MODULE: multipleCommonParents()()(common1 common2)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific class Box<U> {
    public specific init() {}
    public specific func identity(value: U): U { value }
}

specific extend<U> Box<U> {
    public specific func extendIdentity(value: U): U { value }
}

public specific func select<U>(candidate!: U): U { candidate }
public func ordinary(value: Int64): Int64 { value }
