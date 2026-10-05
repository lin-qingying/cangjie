// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（每个 `@When` 条件单独成文件；cjc 对同一次编译里先出现的
// parse 错误（如 `@When[1]`）会终止后续语义诊断，同文件混放取不到其它条件的诊断）：
//   `@When[unknown == "x"]`   → conditional_compilation_not_support_this_condition
//     "conditional compilation have not supported this condition: 'unknown'"（锚条件名首字母 1 个字符）
//   `@When[os > "Windows"]`   → conditional_compilation_not_support_op
//     "conditional compilation 'os' not support this op '>'"（锚条件名首字母 1 个字符）
//   `@When[cjc_version == "1.0"]` → conditional_compilation_not_support_cjc_version_format
//     "cjc version's format should be 'xx.xx.xx'"（锚条件名首字母 1 个字符）；`@When[cjc_version >= "1.0.5"]` 零诊断
//   `@When[arch == 1]`        → conditional_compilation_invalid_condition_value
//     "conditional compilation's condition value should be string literal without interpolation"（锚右值 `1` 1 个字符）
//   `@When[arch == "arm"]`    → conditional_compilation_not_support_builtin_value
//     "builtin condition 'arch' do not support 'arm', supported listed: 'x86_64 aarch64'"（锚条件名首字母 1 个字符）
//   `@When[1]`                → parse_unrecognized_expression_in_when（**parser 阶段** "unrecognized expression '1' in
//     annotation '@When'"，锚表达式 1 个字符）；本项目没有对应诊断名，本文件不写标记
//   `@When[os == "Windows"]` / `@When[os == "Linux"]` → 零诊断（合法对照）
// 取证环境：cjc 1.0.5 自身版本为 1.0.5（`cjc_version` 比较按编译器版本求值），arch=x86_64、os=Windows。
// 本项目：上述条件编译 kind 依次对应 CFIR 的 CONDITIONAL_COMPILATION_NOT_SUPPORT_THIS_CONDITION、
//   _NOT_SUPPORT_OP、_NOT_SUPPORT_CJC_VERSION_FORMAT、_INVALID_CONDITION_VALUE、_NOT_SUPPORT_BUILTIN_VALUE；
// 范围口径：官方 GetBegin 1 字符锚按项目 Diagnostic Range Policy 不镜像；CFIR 与本文件标记
//   统一锚完整条件名（INVALID_CONDITION_VALUE 锚完整右值 token）。

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
// 官方是 parser 阶段诊断（parse_unrecognized_expression_in_when），本项目没有对应诊断名，故不写标记。
@When[1]
func nonComparisonExpression() {}

// FILE: legalControl.cj
@When[os == "Windows"]
func windowsOnly() {}

@When[os == "Linux"]
func linuxOnly() {}
