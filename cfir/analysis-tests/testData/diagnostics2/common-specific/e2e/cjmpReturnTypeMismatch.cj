// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
// 返回类型不同：1.1.3 按不配对处理（双向 NOT_MATCHED），不报返回类型诊断
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

<!NOT_MATCHED!>public common func a(): Int64<!>

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!NOT_MATCHED!>public specific func a(): String { "" }<!>
