// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll

public common class Box {
    public common init()

    public common prop value: Int64 {
        get() { 1 }
    }
}

// MODULE: propertyMemberMatches()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific class Box {
    public specific init() {}

    public specific prop value: Int64 {
        get() { 2 }
    }
}
