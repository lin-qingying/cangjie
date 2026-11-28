// LANGUAGE_VERSION: 1.1.0
// Evidence: review-20261002/mixed-boundaries/mixedDuplicateKind, actual common duplicate kind is function.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package mixed_duplicate_function

public common enum E { A | Value(Int64) }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: specific.cj
// FILE: specific.cj
package mixed_duplicate_function

public specific enum E {
    A | Value(Int64)
    public specific func Value(x: Int64): E { if (x == 0) { A } else { A } }
}

// FILE: unrelated.cj
package mixed_duplicate_function

public func unrelated(): Int64 { 41 }
