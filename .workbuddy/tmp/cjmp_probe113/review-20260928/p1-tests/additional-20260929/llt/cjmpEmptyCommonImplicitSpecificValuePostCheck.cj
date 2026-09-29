// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：review-20260928/p1-cjc-20260929/common_empty_specific_implicit_value。
// specific 非空体初始为 Quest，先配对；随后 Int64 不是 common Unit 的子类型，保留配对并后验报错。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package p1_empty_common_value

public common func value() {}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package p1_empty_common_value

<!RETURN_TYPE_INCOMPATIBLE!>public specific func value() { 2 }<!>
