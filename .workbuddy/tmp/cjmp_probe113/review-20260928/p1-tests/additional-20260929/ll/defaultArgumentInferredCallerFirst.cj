// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_default_first

public common func a(x!: Int64 = 1): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_default_first

// cjc use3 精确探针确认此调用可推断 Int64；LL 首个语义请求只检查本文件。
public func use3() { a() }

// FILE: callee.cj
package cjmp_ll_default_first

public specific func a(x!: Int64): Int64 { x }
