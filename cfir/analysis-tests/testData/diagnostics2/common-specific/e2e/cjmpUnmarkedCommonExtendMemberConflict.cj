// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

common extend Int64 {
    public func duplicate(value: Int64): Int64 { value }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_p

specific extend Int64 {
    public func <!CONFLICTING_OVERLOADS!>duplicate<!>(value: Int64): Int64 { value + 1 }
}
