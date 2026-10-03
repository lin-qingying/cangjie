// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（kind sema_generics_type_variable_not_defined）：
//   `class Box<T> where String <: Named {}`（约束位置写了标准库可见类名而非类型参数）
//     → error: generics type variable 'String' has not defined，锚只覆盖约束名首字母 1 个字符。
//   约束名是本文件可见的类（Local）时官方报同一个 kind、同一个锚点，见 nameInConstraintVisibleClassifier.cj。
// 本项目：官方 kind 在 CfirDiagnosticsList 里没有同名诊断，语义上对应 NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER。
interface Named {}

class Box<T> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>S<!>tring <: Named {}
