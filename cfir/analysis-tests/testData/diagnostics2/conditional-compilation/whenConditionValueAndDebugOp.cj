// RUN_PIPELINE_TILL: FRONTEND
// WHEN_ENV: backend=cjnative,arch=x86_64,os=Windows,cjc_version=1.0.5,debug=false,test=false
// 证据：官方 cjc 1.1.3 隔离探针确认诊断 kind；官方单字符 source range 只用作诊断归属证据，
// CFIR 按 PSI / LightTree 的完整 identifier 或 RHS expression 定位，不裁剪 token。
//
//   `func withVar(os: String) { @When[os == os] let a = 1; let _ = a }`（右值是变量而非字符串字面量）
//     → conditional_compilation_invalid_condition_value
//       "conditional compilation's condition value should be string literal without interpolation"（CFIR 锚完整右值 `os`）
//     条件非法只报告失败事实，不将声明当作 false 剪除；后续读取 `a` 不产生级联未解析诊断。
//   `@When[debug == "true"]`（debug 不支持比较运算符）
//     → conditional_compilation_not_support_op "conditional compilation 'debug' not support this op '=='"
//       （CFIR 锚完整 `debug`）
// 本项目：分别对应 CONDITIONAL_COMPILATION_INVALID_CONDITION_VALUE 与 CONDITIONAL_COMPILATION_NOT_SUPPORT_OP；
//   非法条件只产生上述诊断，保留声明及后续引用。

// FILE: valueIsVariable.cj
func withVar(os: String) {
    @When[os == <!CONDITIONAL_COMPILATION_INVALID_CONDITION_VALUE!>os<!>]
    let a = 1
    let _ = a
}

// FILE: debugCompared.cj
@When[<!CONDITIONAL_COMPILATION_NOT_SUPPORT_OP!>debug<!> == "true"]
func debugCompared() {}
