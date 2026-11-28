// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll

public common class Box {
    public common init()
    public common func shared(value: Int64): Int64
    public common func shared(value: Bool): Bool
}

// MODULE: memberFirstMatch()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific class Box {
    public specific init() {}
    public specific func shared(value: Int64): Int64 { value }
    public specific func shared(value: Bool): Bool { value }
    public specific func shared(value: Int64): Int64 { value }
}
