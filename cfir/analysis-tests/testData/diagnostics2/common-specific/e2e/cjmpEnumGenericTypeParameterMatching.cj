// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 two-stage probe: .workbuddy/tmp/cjmp_probe113/phase5_enum_generic_{common,specific}.cj
// Matching enum constructor parameter types through the common/specific enum type parameters is accepted with no diagnostics.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common enum Box<T> {
    | Value(T)
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific enum Box<U> {
    | Value(U)
}
