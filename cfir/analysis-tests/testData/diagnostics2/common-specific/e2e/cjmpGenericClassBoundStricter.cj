// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 two-stage probe: .workbuddy/tmp/cjmp_probe113/phase5_nominal_generic_{common,specific}.cj
// A specific class may not narrow the common type-parameter bound; local override diagnostic anchor is the constraint item.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common interface Bound {}

public common class Box<T> {
    public common init() {}
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class Box<U> where <!GENERIC_CONSTRAINT_NOT_LOOSER!>U <: Bound<!> {
    public specific init() {}
}
