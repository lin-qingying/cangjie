// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：review-20260928/p1-cjc-20260929/common_value_specific_empty。
// specific 空体预检为 Unit，与 common 已推断 Int64 预配对失配；不能仅凭缺少显式返回类型接受。
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

<!NOT_MATCHED!>public specific func value() {}<!>
