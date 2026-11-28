// LANGUAGE_VERSION: 1.2.0-alpha.20
// MODULE: commonBase
// CJMP_MODE: COMMON
// FILE: commonBase.cj
package cjmp_ll

public common class Box<T> {
    public common init()
    public common func identity(value: T): T
}

// MODULE: common1()()(commonBase)
// CJMP_MODE: COMMON
// FILE: common1.cj
package cjmp_ll

// MODULE: common2()()(commonBase)
// CJMP_MODE: COMMON
// FILE: common2.cj
package cjmp_ll

// MODULE: multipleCommonParentsSharedAncestor()()(common1 common2)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific class Box<U> {
    public specific init() {}
    public specific func identity(value: U): U { value }
}
