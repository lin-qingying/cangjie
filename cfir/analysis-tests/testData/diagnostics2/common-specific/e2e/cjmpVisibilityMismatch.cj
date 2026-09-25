// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
// 可见性不同：修饰符诊断（仍配对，无 NOT_MATCHED）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common func a(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!SPECIFIC_HAS_DIFFERENT_MODIFIER!>specific func a(): Int64 { 1 }<!>
