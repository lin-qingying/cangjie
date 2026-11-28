// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 `cjmp_probe113/probe_results6.txt`, CASE specific_toplevel_bodyless:
// `specific func Pf()` reports the missing return type on `func` (specific.cj:7:17). The local
// Shared parser owns this syntax error; this fixture only expects the unmatched semantic declaration.

// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common func Platform(): String {
    "Common"
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!NOT_MATCHED!>public specific func Pf()<!>
