// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_parameter_name_mismatch

public common func choose(commonValue!: Int64): Int64

// MODULE: parameterNameMismatchKeepsCommonCallableCallerFirst()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_parameter_name_mismatch

// The first semantic request must keep the common overload visible to this named call.
public func use(): Int64 { choose(commonValue: 1) }

// FILE: callee.cj
package cjmp_ll_parameter_name_mismatch

public specific func choose(specificValue!: Int64): Int64 { specificValue }
