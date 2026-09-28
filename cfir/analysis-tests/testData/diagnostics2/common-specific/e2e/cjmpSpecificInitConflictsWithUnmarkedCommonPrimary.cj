// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 T16: the specific secondary init conflicts with the common class's unmarked primary constructor;
// the init remains unmatched because a primary constructor cannot be a common/specific counterpart.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class P {
    public P(x: Int64) {}
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_p

public specific class P {
    <!NOT_MATCHED!>public specific <!CONFLICTING_OVERLOADS!>init<!>(x: Int64) {}<!>
}
