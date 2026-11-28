// LANGUAGE_VERSION: 1.1.0
// Evidence: review-20261002/mixed-boundaries/nonExhaustiveFunction, official cjc 1.1.3 exits 0 / 0.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package mixed_unmatched_function

public common enum E { A | ... }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: specific.cj
// FILE: specific.cj
package mixed_unmatched_function

public specific enum E {
    A
    public specific func Extra(x: Int64): E { if (x == 0) { A } else { A } }
}

// FILE: unrelated.cj
package mixed_unmatched_function

public func unrelated(): Int64 { 41 }
