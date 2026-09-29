// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：review-20260928/p1-cjc-20260929/both_implicit_good，双方 exit 0、零诊断。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package p1_both_implicit_good

public common func value() { 1 }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package p1_both_implicit_good

public specific func value() { 2 }
public func use() { value() }
public func typeWitness(): Int64 { use() }
