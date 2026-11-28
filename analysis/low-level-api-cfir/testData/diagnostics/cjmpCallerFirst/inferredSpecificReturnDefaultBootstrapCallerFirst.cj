// LANGUAGE_VERSION: 1.1.0-alpha.66
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_inferred_default_bootstrap

public common func inferred(x!: Int64 = 1): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_inferred_default_bootstrap

public func use() { inferred() }

// FILE: callee.cj
package cjmp_ll_inferred_default_bootstrap

public platform func inferred(x!: Int64) { true }
