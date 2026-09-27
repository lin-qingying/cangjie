// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// FILE: common.cj
package cjmp_ll

public common class Box {
    public common init()
    public common func shared(): Int64
}

// MODULE: memberFirstMatch()()(common)
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific class Box {
    public specific init() {}
    public specific func shared(): Int64 { 1 }
    public specific func shared(): Int64 { 2 }
}
