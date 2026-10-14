// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：review-20260928/p1-cjc-20260929/common_value_specific_empty。
// 官方 PreTypeCheck 先建立 CJMP 配对，PostTypeCheck 再比较函数体推断的返回类型；Unit 与 Int64 不兼容。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package p1_value_common_empty

public common func value() { 1 }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package p1_value_common_empty

<!RETURN_TYPE_INCOMPATIBLE!>public specific func value() {}<!>
