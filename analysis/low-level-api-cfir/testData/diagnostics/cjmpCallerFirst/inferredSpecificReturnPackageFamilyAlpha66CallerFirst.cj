// LANGUAGE_VERSION: 1.1.0-alpha.66
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_inferred_package_family_alpha66

public common func inferred(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_inferred_package_family_alpha66

public func caller(): Int64 { 0 }

// FILE: callee.cj
package cjmp_ll_inferred_package_family_alpha66

public platform func inferred() { true }
