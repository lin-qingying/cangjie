// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_shadow_first

public common func platform(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_shadow_first

// 第一个语义请求只收集 caller.cj 的诊断，不能预热 callee.cj。
public func use4(): Int64 { platform() }

// FILE: callee.cj
package cjmp_ll_shadow_first

public specific func platform(): Int64 { 2 }
