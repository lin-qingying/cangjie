// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_explicit_return_mismatch

public common open class Base {}
public common func explicit(value: Int64): Base

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_explicit_return_mismatch

public func use(): Base { explicit(1) }

// FILE: callee.cj
package cjmp_ll_explicit_return_mismatch

public specific func explicit(value: Int64): Int64 { 1 }
