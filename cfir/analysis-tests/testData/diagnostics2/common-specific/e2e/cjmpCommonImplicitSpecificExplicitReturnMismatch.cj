// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：review-20260928/p1-cjc-20260929/common_implicit_specific_explicit_bad。
// common 已推断 Int64；specific 显式 String 在预配对时失配。common 有默认体，不报 common NOT_MATCHED。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package p1_common_implicit_explicit_bad

public common func value() { 1 }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package p1_common_implicit_explicit_bad

<!NOT_MATCHED!>public specific func value(): String { "bad" }<!>
