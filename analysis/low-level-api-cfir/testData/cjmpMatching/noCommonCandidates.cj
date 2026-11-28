// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll

public common class Other {}

// MODULE: noCommonCandidates()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific class Box {
    public specific func onlyHere(): Int64 { 1 }
}
