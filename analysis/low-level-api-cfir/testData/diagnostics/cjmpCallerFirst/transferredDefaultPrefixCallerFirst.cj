// LANGUAGE_VERSION: 1.1.0
// MODULE: commonFirst
// CJMP_MODE: COMMON
// FILE: commonFirst.cj
package cjmp_ll_default_first

public common func choose(first!: Int64 = 1, different!: Int64): Int64 { first + different }

// MODULE: commonSecond
// CJMP_MODE: COMMON
// FILE: commonSecond.cj
package cjmp_ll_default_first

public common func choose(first!: Int64, second!: Int64): Int64

// MODULE: transferredDefaultPrefixCallerFirst()()(commonFirst commonSecond)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_default_first

public func use(): Int64 { choose(second: 2) }

// FILE: callee.cj
package cjmp_ll_default_first

public specific func choose(first!: Int64, second!: Int64): Int64 { first + second }
