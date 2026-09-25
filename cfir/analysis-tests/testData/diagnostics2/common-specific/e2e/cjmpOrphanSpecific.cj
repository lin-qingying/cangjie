// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
// specific 声明无 common 对应：specific 方向 NOT_MATCHED
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common func a(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific func a(): Int64 { 1 }
<!NOT_MATCHED!>public specific func z(): Int64 { 2 }<!>
