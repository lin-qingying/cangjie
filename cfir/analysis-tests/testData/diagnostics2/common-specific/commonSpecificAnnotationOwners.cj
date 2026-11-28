// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 e7-annotation-owner：CallingConv 的 CFFI 诊断与 CJMP 禁止诊断独立存在。
package e7_annotation_owners

@EnsurePreparedToMock
public func ordinary(): Int64 { 1 }

<!COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED!>@EnsurePreparedToMock<!>
public common func denied(): Int64 { 1 }

<!COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED, ONLY_CFUNC_CAN_USE_ANNOTATION!>@CallingConv[C]<!>
public common func wrongConvention(): Int64 { 1 }
