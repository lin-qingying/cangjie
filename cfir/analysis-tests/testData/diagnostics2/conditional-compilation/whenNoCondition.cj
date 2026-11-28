// RUN_PIPELINE_TILL: FRONTEND
// WHEN_ENV: backend=cjnative,arch=x86_64,os=Windows,cjc_version=1.0.5,debug=false,test=false
// 证据：官方 cjc 1.0.5 实测：`@When[]`（条件列表为空）→ error: expected expression after '[', found ']'（解析层拒绝，锚 `[` 之后的空位）。
// 占位说明：CFIR 的 evaluator 对空条件返回 CONDITIONAL_COMPILATION_NOT_HAVE_CONDITION_EXPR，
// 但本仓库 parser 在 `@When[]` 上先给出解析错误，`CfirConditionalCompilationFailures` 为空，
// 因此 CFIR 当前不产出该诊断；本用例作为「官方 cjc 有解析错误、CFIR 无对应诊断」的回归基线。
@When[]
func noCondition() {}
