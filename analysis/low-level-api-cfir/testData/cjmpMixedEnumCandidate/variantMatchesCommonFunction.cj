// LANGUAGE_VERSION: 1.1.0
// Evidence: review-20261002/enum-plan-main/mixed_body; the successful common function is NOT static.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_enum_mixed_body_probe

public common enum E {
    A | ...
    public common func Value(x: Int64): E { A }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: specific.cj
package cjmp_enum_mixed_body_probe

public specific enum E { A | Value(Int64) }

// FILE: caller.cj
package cjmp_enum_mixed_body_probe

public func make(): E { E.Value(1) }
