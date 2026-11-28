// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_uncalled_first

public common func used(): Int64
public common func unused(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_uncalled_first

public func caller(): Int64 { used() }

// FILE: callee.cj
package cjmp_ll_uncalled_first

public specific func used(): Int64 { 1 }

// FILE: uncalled.cj
package cjmp_ll_uncalled_first

public specific func unused(): Int64 { 2 }
