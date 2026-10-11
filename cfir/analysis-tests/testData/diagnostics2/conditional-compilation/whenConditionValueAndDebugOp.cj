// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测：
//   `func withVar(os: String) { @When[os == os] let a = 1; let _ = a }`（右值是变量而非字符串字面量）
//     → conditional_compilation_invalid_condition_value
//       "conditional compilation's condition value should be string literal without interpolation"（锚**右值** `os` 首字母 1 个字符）
//     条件失效后被剔除的局部变量 `a` 在后续读取处官方零诊断。
//   `@When[debug == "true"]`（debug 不支持比较运算符）
//     → conditional_compilation_not_support_op "conditional compilation 'debug' not support this op '=='"
//       （锚条件名首字母 1 个字符）
// 本项目：分别对应 CONDITIONAL_COMPILATION_INVALID_CONDITION_VALUE 与 CONDITIONAL_COMPILATION_NOT_SUPPORT_OP；
//   CFIR 对条件失效后读取被剔除变量会另报 UNRESOLVED_REFERENCE，官方没有这条诊断，故此处不写该标记；
//   CFIR 把条件整体标在一处，与官方的 1 字符锚不同（实现缺口）。

// FILE: valueIsVariable.cj
func withVar(os: String) {
    @When[os == <!CONDITIONAL_COMPILATION_INVALID_CONDITION_VALUE!>o<!>s]
    let a = 1
    let _ = a
}

// FILE: debugCompared.cj
@When[<!CONDITIONAL_COMPILATION_NOT_SUPPORT_OP!>d<!>ebug == "true"]
func debugCompared() {}
