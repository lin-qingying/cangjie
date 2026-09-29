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

// cjc use4 精确探针确认此调用可推断 Int64；不得为 LL 测试预热 callee。
public func use4() { platform() }

// FILE: callee.cj
package cjmp_ll_shadow_first

public specific func platform(): Int64 { 2 }
