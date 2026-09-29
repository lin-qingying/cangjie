// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：review-20260928/p1-cjc-20260929/specific_error_type。
// 与主体后续错误不同：签名显式 Missing 在预配对时已错误，不得当成 Quest，common/specific 双方失配。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package p1_specific_error_type

<!NOT_MATCHED!>public common func value(): Int64<!>

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package p1_specific_error_type

<!NOT_MATCHED!>public specific func value(): <!UNDECLARED_TYPE_NAME!>Missing<!> {}<!>
