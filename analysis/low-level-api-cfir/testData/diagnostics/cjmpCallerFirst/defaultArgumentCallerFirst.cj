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

// 显式返回类型隔离 A2 的跨文件惰性读取，与 A1 的阶段顺序用例互补。
public func use3(): Int64 { a() }

// FILE: callee.cj
package cjmp_ll_default_first

public specific func a(x!: Int64): Int64 { x }
