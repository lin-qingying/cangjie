// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_parallel_implicit

public common func first() { 1 }
public common func second() { first() }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll_parallel_implicit

public specific func first(): Int64 { 2 }
public specific func second(): Int64 { 3 }
