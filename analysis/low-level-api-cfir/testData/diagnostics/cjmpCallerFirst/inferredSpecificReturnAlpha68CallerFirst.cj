// LANGUAGE_VERSION: 1.1.0-alpha.68
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_return_alpha_68

public common func inferred(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_return_alpha_68

public func use() { inferred() }

// FILE: callee.cj
package cjmp_ll_return_alpha_68

public platform func inferred() { true }
