// LANGUAGE_VERSION: 1.1.0
// Evidence: review-20261002/mixed-boundaries/reverse, official cjc 1.1.3 exits 0 / 0.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package mixed_reverse

public common enum E { A | Value(Int64) }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: specific.cj
package mixed_reverse

public specific enum E {
    A
    public specific func Value(x: Int64): E { if (x == 0) { A } else { A } }
}

// FILE: caller.cj
package mixed_reverse

public func make(e: E): E { e.Value(1) }

// FILE: unrelated.cj
package mixed_reverse

public func unrelated(): Int64 { 41 }
