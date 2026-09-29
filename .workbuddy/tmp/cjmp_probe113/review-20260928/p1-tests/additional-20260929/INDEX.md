# P1 附加 fixture：cjc 精确结果到项目期望

本目录只保存 scratch 草稿。没有改正式 testData、Kotlin 测试或生成器，没有运行 Gradle。

依据为 [p1-cjc-20260929/run-results.json](../../p1-cjc-20260929/run-results.json) 中的官方 cjc 1.1.3 原始结果。本轮保留探针原有声明文本和包名，仅添加模块指令与项目诊断 markers；LL 两变体基于已经正式编译的 caller-first fixture 移除 caller 返回类型，沿用其模块包名。

## 可直接移入的文件

`llt/` 中 7 个文件对应正式目录 `cfir/analysis-tests/testData/diagnostics2/common-specific/e2e/`；需由主线程按现有生成流程更新三个 Diagnostics2 生成测试。`ll/` 中 2 个文件对应 `analysis/low-level-api-cfir/testData/diagnostics/cjmpCallerFirst/`，手写类只需加入 [两个测试方法](CfirCjmpCallerFirstDiagnosticsTest.methods.kt.fragment)，复用已经存在的 assertCallerFirst，不修改其准备顺序或断言。

| 草稿 | 官方精确用例 | 项目诊断期望 |
|---|---|---|
| [cjmpBothImplicitReturnTypesMatch.cj](llt/cjmpBothImplicitReturnTypesMatch.cj) | both_implicit_good | 无诊断；typeWitness 要求调用结果为 Int64 |
| [cjmpCommonImplicitSpecificExplicitReturnMismatch.cj](llt/cjmpCommonImplicitSpecificExplicitReturnMismatch.cj) | common_implicit_specific_explicit_bad | specific 完整声明 NOT_MATCHED；common 默认体不要求实现 |
| [cjmpEmptyBodiesMatchAsUnit.cj](llt/cjmpEmptyBodiesMatchAsUnit.cj) | both_empty_unit | 无诊断；typeWitness 返回 Unit |
| [cjmpEmptyCommonImplicitSpecificValuePostCheck.cj](llt/cjmpEmptyCommonImplicitSpecificValuePostCheck.cj) | common_empty_specific_implicit_value | specific 完整声明 RETURN_TYPE_INCOMPATIBLE，无 NOT_MATCHED |
| [cjmpCommonValueSpecificEmptyBodyMismatch.cj](llt/cjmpCommonValueSpecificEmptyBodyMismatch.cj) | common_value_specific_empty | specific 完整声明 NOT_MATCHED |
| [cjmpImplicitSpecificBodyErrorPreservesMatch.cj](llt/cjmpImplicitSpecificBodyErrorPreservesMatch.cj) | specific_implicit_error | absent 完整 token 的 UNRESOLVED_REFERENCE + specific 完整声明 RETURN_TYPE_INCOMPATIBLE；两侧无 NOT_MATCHED |
| [cjmpExplicitSpecificErrorTypeDoesNotMatch.cj](llt/cjmpExplicitSpecificErrorTypeDoesNotMatch.cj) | specific_error_type | Missing 完整 token 的 UNDECLARED_TYPE_NAME + common/specific 两个完整声明的 NOT_MATCHED |
| [defaultArgumentInferredCallerFirst.cj](ll/defaultArgumentInferredCallerFirst.cj) | use3（跨文件 LL 变体） | 无诊断；调用必须绑定 callee.cj specific a，call/caller 类型都为 Int64 |
| [shadowedCommonInferredCallerFirst.cj](ll/shadowedCommonInferredCallerFirst.cj) | use4（跨文件 LL 变体） | 无诊断；调用必须绑定 callee.cj specific platform，call/caller 类型都为 Int64 |

## Fixture Gate 依据

- 这是按官方结果增加期望，不是把当前红测试 actual 写回 fixture。当前正式 caller-first 和推断 shadow 红结果见 [red-results-20260929.json](../red-results-20260929.json)。本目录未运行，不能填为 green。
- 诊断名使用项目表面：`sema_not_matched` → NOT_MATCHED；`sema_return_type_incompatible` → RETURN_TYPE_INCOMPATIBLE；`sema_undeclared_identifier` → UNRESOLVED_REFERENCE；`sema_undeclared_type_name` → UNDECLARED_TYPE_NAME。对应工厂定义位于 `CfirDiagnosticsList.kt`；既有 `cjmpInferredReturnTypePostCheck.cj`、`cjmpReturnTypeMismatch.cj`、`cjmpEnumConstructorUnresolvedType.cj` 已使用这些名称和嵌套 marker 形式。
- 官方 NOT_MATCHED / RETURN_TYPE_INCOMPATIBLE 的 MainHint 从 specific.cj 的 `public` 开始覆盖声明，草稿使用完整函数声明范围，保留项目现有声明级定位契约。未知标识符和类型的官方位置只覆盖极窄范围；按仓库 skill 的 Diagnostic Range Policy，marker 覆盖完整 absent/Missing token，不采用单字符高亮。
- 已搜索同族 common-specific/e2e：既有 `cjmpInferredReturnTypePostCheck.cj` 覆盖双方非空体隐式返回一致/不一致，但没有空体 Unit、common implicit/specific explicit 或主体错误后验反例；新增七份不改既有 fixture 的期望。
- 两段式官方探针的 common 已经过编译，返回类型已经具体化。它们不证明“LL 两端同时仍为 Quest”的具体内部状态；LL 中该情况仍按 authority 的官方源码判据处理。不得据官方 CJO NOT_MATCHED 结果强迫所有尚未具体化的 LL 配对提前失败。

## 初始签名与后验错误必须区分

`specific_implicit_error` 在预配对阶段是合法的待推断返回类型，随后 absent 才使主体出现错误。官方输出是 UNRESOLVED_REFERENCE 与 RETURN_TYPE_INCOMPATIBLE；不能把后续错误改写成 TypeNotResolved 失配，也不能清掉已有 counterpart。与它对照的 `specific_error_type` 则在预配对时显式返回类型 Missing 就已错误，双方确实 NOT_MATCHED。

`common_value_specific_empty` 的 specific 虽然没有显式返回标注，但空体在官方预检中就是 Unit，不属于上述待推断状态；其 NOT_MATCHED 不能改成后验 RETURN_TYPE_INCOMPATIBLE。`common_empty_specific_implicit_value` 则正好相反：specific 非空体先以 Quest 配对，后验发现 Int64/Unit 不兼容。

## 十二组官方结果的落点

use3/use4 已有正式 A1 fixture，本次补 LL 隐式 caller 变体；both_implicit_good 本次补单独的成功和类型 witness；both_implicit_bad 由既有 `cjmpInferredReturnTypePostCheck.cj` 覆盖同族，本次不复制；其余空体、显式不兼容、签名错误、主体错误由上表覆盖。common_bodyless_untyped / specific_bodyless_untyped 属于 parse 前置门，本次没有扩大到解析期 fixture；对应官方 JSON 留在原探针目录，不能把无 body 的声明混同为空 body。

## 已做的静态核对

7 份 LLT 草稿移除新增指令/注释和 inline markers 后，与各自官方探针 common.cj + specific.cj 的仓颉文本逐一比较一致（仅忽略空白差异）。所有 marker 的嵌套均平衡；本索引 12 个链接均存在。此检查没有调用 Gradle，也不代表正式 fixture 已通过本仓解析或诊断验证。
