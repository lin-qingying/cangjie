// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// P1/A1 草稿：官方 v1.1.3 TypeChecker.cpp:2027–2047 在函数体前配对；
// CheckCJMP.cpp:948–963 将 common 默认参数复制到 specific。尚未运行本草稿。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_default_before_implicit

public common func a(x!: Int64 = 1): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_default_before_implicit

public specific func a(x!: Int64): Int64 { x }

// 保留省略返回类型；显式写 Int64 会绕过 A1 的 IMPLICIT_TYPES 消费路径。
public func use3() { a() }
