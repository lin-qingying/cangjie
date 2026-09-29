// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：review-20260928/p1-cjc-20260929/specific_implicit_error。
// 初始 Quest 已配对；后续 absent 错误保留配对，并报告返回类型不兼容，双方均不得新增 NOT_MATCHED。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package p1_specific_implicit_error

public common func value(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package p1_specific_implicit_error

<!RETURN_TYPE_INCOMPATIBLE!>public specific func value() { <!UNRESOLVED_REFERENCE!>absent<!> }<!>
