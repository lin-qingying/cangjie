// LANGUAGE_VERSION: 1.2.0-alpha.20
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_shared_context

public common func shared(): Int64

// FILE: commonError.cj
package cjmp_ll_shared_context

public common func broken(): Int64 { missingName() }

// MODULE: matched()()(common)
// CJMP_MODE: SPECIFIC
// FILE: matched.cj
package cjmp_ll_shared_context

public specific func shared(): Int64 { 1 }

// MODULE: unmatched()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: unmatched.cj
package cjmp_ll_shared_context

public specific func unrelated(): Int64 { 2 }
