// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_specific_return_post_check

public common open class Base {}
public common func inferred(value: Int64): Base

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_specific_return_post_check

// The first request analyzes only this caller; inferred() starts with an unresolved specific return type.
public func use() { inferred(1) }

// FILE: callee.cj
package cjmp_ll_specific_return_post_check

public specific func inferred(value: Int64) { 1 }
