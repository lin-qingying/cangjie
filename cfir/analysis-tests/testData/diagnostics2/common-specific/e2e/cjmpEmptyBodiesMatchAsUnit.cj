// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：review-20260928/p1-cjc-20260929/both_empty_unit，双方 exit 0、零诊断。
// 无返回类型的空函数体在预检即为 Unit；不是等待主体推断的 Quest。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package p1_both_empty_unit

public common func value() {}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package p1_both_empty_unit

public specific func value() {}
public func typeWitness(): Unit { value() }
