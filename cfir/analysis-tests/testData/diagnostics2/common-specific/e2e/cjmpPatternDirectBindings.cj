// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 两阶段官方探针：review-20261002/var-pattern-merge/patternAll，common 与 specific 均无诊断。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_pattern_direct

public common let a: Int64
public common let b: Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_pattern_direct

public specific let (a, b) = (1, 2)
