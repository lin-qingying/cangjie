// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 对缺少类型实参的泛型接口只报告泛型实参诊断。
package e9_generic_super
interface Generic<T> {}
common extend Int64 <: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Generic<!> {}
