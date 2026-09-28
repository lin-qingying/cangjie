// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// FILE: common.cj
package cjmp_ll

public common func shared(): Int64

// MODULE: parallelFirstMatch()()(common)
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific func shared(): Int64 { 1 }
public specific func shared(): Int64 { 2 }
