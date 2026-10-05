// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（kind sema_generics_type_variable_not_defined）：
//   `class Box<T> where Local <: Named {}`（约束位置写了本文件可见的类名而非类型参数）
//     → error: generics type variable 'Local' has not defined，锚只覆盖约束名首字母 1 个字符。
//   约束名是标准库可见类名（String）时官方报同一个 kind，见 nameInConstraintVisibleClass.cj。
// 本项目：官方 kind 在 CfirDiagnosticsList 里没有同名诊断，语义上对应 NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER。
interface Named {}

class Local {}

class Box<T> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>Local<!> <: Named {}
