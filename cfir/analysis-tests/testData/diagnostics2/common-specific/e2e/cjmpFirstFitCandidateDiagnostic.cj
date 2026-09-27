// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// First common candidate reports a parameter mismatch; the second candidate still binds the specific member.
// MODULE: common1
// CJMP_MODE: COMMON
// FILE: common1.cj
package cjmp_p

common extend Int64 {
    public common func select(candidate!: Int64): Int64 { candidate }
}

// MODULE: common2
// CJMP_MODE: COMMON
// FILE: common2.cj
package cjmp_p

common extend Int64 {
    public common func select(selected!: Int64): Int64 { selected }
}

// MODULE: specific()()(common1 common2)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

specific extend Int64 {
    public specific func select(<!SPECIFIC_HAS_DIFFERENT_PARAMETER!>selected!: Int64<!>): Int64 { selected }
}
