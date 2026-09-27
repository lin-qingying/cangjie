// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// Official CheckCJMP::MustMatchWithSpecific: an unpaired COMMON_WITH_DEFAULT enum does not require specific constructor counterparts.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common enum Defaulted {
    | Existing
    | Future
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

func keepSpecificSession(): Unit { }
