// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// Invalid common extend keys are excluded before merging and cannot suppress a valid matching owner.
// MODULE: commonInvalid
// CJMP_MODE: COMMON
// FILE: invalid.cj
package cjmp_p

common extend <!UNDECLARED_TYPE_NAME, UNDECLARED_TYPE_NAME!>MissingTarget<!> {
    public common func unrelated(): Int64 { 1 }
}

// MODULE: commonValid
// CJMP_MODE: COMMON
// FILE: valid.cj
package cjmp_p

common extend Int64 {
    public common func select(value: Int64): Int64 { value }
}

// MODULE: specific()()(commonInvalid commonValid)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

specific extend Int64 {
    public specific func select(value: Int64): Int64 { value }
}
