// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// P1/A1 草稿：官方 v1.1.3 TypeChecker.cpp:2027–2047 在函数体前配对；
// 已配对的 specific 调用替代 common。无返回类型的函数由末项调用推断 Int64。尚未运行本草稿。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_shadow_before_implicit

public common func platform(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_shadow_before_implicit

public specific func platform(): Int64 { 2 }

// 保留省略返回类型；common 与 specific 不得同时存活为歧义候选。
public func use4() { platform() }
