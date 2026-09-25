// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
// 默认值双侧：默认值诊断 + 双向 NOT_MATCHED
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

<!NOT_MATCHED!>public common func a(x!: Int64 = 1): Int64<!>

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!NOT_MATCHED!>public specific func a(<!CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES!>x!: Int64 = 2<!>): Int64 { x }<!>
