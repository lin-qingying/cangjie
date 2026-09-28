// LANGUAGE_VERSION: 1.1.0
// MODULE: common1
// FILE: common1.cj
package cjmp_ll

public common func select(candidate!: Int64): Int64 { candidate }

// MODULE: common2
// FILE: common2.cj
package cjmp_ll

public common func select(selected!: Int64): Int64 { selected }

// MODULE: firstFitCandidateDiagnostic()()(common1 common2)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_ll

public specific func select(selected!: Int64): Int64 { selected }
