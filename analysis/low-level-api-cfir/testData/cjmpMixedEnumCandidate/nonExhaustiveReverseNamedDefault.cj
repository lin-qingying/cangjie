// LANGUAGE_VERSION: 1.1.0
// Evidence: review-20261002/mixed-boundaries/reverseNamedDefault.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package mixed_reverse_default

public common enum E { A | Value(Int64) | ... }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: specific.cj
// FILE: specific.cj
package mixed_reverse_default

public specific enum E {
    A
    public specific func Value(x!: Int64 = 1): E { if (x == 0) { A } else { A } }
}

// FILE: unrelated.cj
package mixed_reverse_default

public func unrelated(): Int64 { 41 }
