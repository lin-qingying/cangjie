// RUN_PIPELINE_TILL: FRONTEND
// WHEN_ENV: backend=cjnative,arch=x86_64,os=Windows,cjc_version=1.0.5,debug=false,test=false
// 证据：官方 cjc 1.1.3 隔离探针确认各条件 kind；其单字符 source range 只用作诊断归属证据，
// CFIR 依据 PSI / LightTree 完整表达式 source 定位（完整条件名或 RHS 表达式），不裁剪 token。
// cjc 对同一次编译里先出现的
// parse 错误（如 `@When[1]`）会终止后续语义诊断，同文件混放取不到其它条件的诊断）：
//   `@When[unknown == "x"]`   → conditional_compilation_not_support_this_condition
//     "conditional compilation have not supported this condition: 'unknown'"（CFIR 锚完整 `unknown`）
//   `@When[os > "Windows"]`   → conditional_compilation_not_support_op
//     "conditional compilation 'os' not support this op '>'"（CFIR 锚完整 `os`）
//   `@When[cjc_version == "1.0"]` → conditional_compilation_not_support_cjc_version_format
//     "cjc version's format should be 'xx.xx.xx'"（CFIR 锚完整 `cjc_version`）；`@When[cjc_version >= "1.0.5"]` 零诊断
//   `@When[arch == 1]`        → conditional_compilation_invalid_condition_value
//     "conditional compilation's condition value should be string literal without interpolation"（CFIR 锚完整右值 `1`）
//   `@When[arch == "arm"]`    → conditional_compilation_not_support_builtin_value
//     "builtin condition 'arch' do not support 'arm', supported listed: 'x86_64 aarch64'"（CFIR 锚完整 `arch`）
//   `@When[1]`                → 官方 parse_unrecognized_expression_in_when；CFIR 复用含义相同的
//     CONDITIONAL_COMPILATION_INVALID_CONDITION_EXPR，并锚完整表达式 `1`
//   `@When[os == "Windows"]` / `@When[os == "Linux"]` → 零诊断（合法对照）
// 官方隔离探针使用 cjc 1.1.3，arch=x86_64、os=Windows；cjc_version 按编译器版本求值。
// WHEN_ENV 固定 CFIR 的测试环境版本为 1.0.5，使正向版本对照稳定成立；这是注入值，不代表本机 cjc 版本。
// 本项目：上述条件编译 kind 依次对应 CFIR 的 CONDITIONAL_COMPILATION_NOT_SUPPORT_THIS_CONDITION、
//   _NOT_SUPPORT_OP、_NOT_SUPPORT_CJC_VERSION_FORMAT、_INVALID_CONDITION_VALUE、_NOT_SUPPORT_BUILTIN_VALUE、
//   _INVALID_CONDITION_EXPR。CFIR 锚完整 token / 表达式，不裁剪单个 token。

// FILE: unknownCondition.cj
@When[<!CONDITIONAL_COMPILATION_NOT_SUPPORT_THIS_CONDITION!>unknown<!> == "x"]
func unknownCondition() {}

// FILE: unsupportedOperator.cj
@When[<!CONDITIONAL_COMPILATION_NOT_SUPPORT_OP!>os<!> > "Windows"]
func unsupportedOperator() {}

// FILE: badVersionFormat.cj
@When[<!CONDITIONAL_COMPILATION_NOT_SUPPORT_CJC_VERSION_FORMAT!>cjc_version<!> == "1.0"]
func badVersion() {}

// FILE: goodVersionFormat.cj
@When[cjc_version >= "1.0.5"]
func goodVersion() {}

// FILE: nonLiteralValue.cj
@When[arch == <!CONDITIONAL_COMPILATION_INVALID_CONDITION_VALUE!>1<!>]
func nonLiteralValue() {}

// FILE: unsupportedBuiltinValue.cj
@When[<!CONDITIONAL_COMPILATION_NOT_SUPPORT_BUILTIN_VALUE!>arch<!> == "arm"]
func unsupportedBuiltinValue() {}

// FILE: nonComparisonExpression.cj
// 官方是 parser 阶段诊断（parse_unrecognized_expression_in_when）；CFIR 使用含义等价的条件表达式诊断。
@When[<!CONDITIONAL_COMPILATION_INVALID_CONDITION_EXPR!>1<!>]
func nonComparisonExpression() {}

// FILE: legalControl.cj
@When[os == "Windows"]
func windowsOnly() {}

@When[os == "Linux"]
func linuxOnly() {}
