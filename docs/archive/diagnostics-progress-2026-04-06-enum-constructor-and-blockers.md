# Diagnostics Progress Update (2026-04-06)

关联文档：
- `docs/diagnostics-gap-vs-official-cpp-sema-detailed.md`
- `docs/diagnostics-gap-vs-official-cpp-sema-status-2026-04-06.md`
- `cfir/analysis-tests/diagnostics-coverage-gap-vs-cpp.md`

## 本轮新增进展

- 已完成 `enum constructor` 结构修正。
- `CfirEnumConstructor` 不再把 payload 语义折叠进 `returnTypeRef`。
- 当前模型改为：
  - 显式 `valueParameters`
  - 正确 owner enum `returnTypeRef`
- PSI / LightTree / 反序列化 / resolve / pattern legality / conflict signature 已统一切换到这份单一真相表。

## 已验证结果

- `./gradlew :cfir:analysis-tests:test --tests "org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisDiagnosticsTestGenerated*"` 通过。
- `./gradlew :cfir:raw-cfir:psi2cfir:test --tests "*testEnumDeclaration"` 通过。
- `./gradlew :cfir:raw-cfir:light-tree2cfir:test --tests "*testEnumDeclaration"` 通过。

## 当前阻塞

### 1. `NOT_OVERLOAD_IN_MATCH`

- 当前 parser / pattern 建模仍把 `case ident` 优先归类为 binding pattern。
- `case Qualified.Name` 也没有稳定进入 const-pattern equality 检查链路。
- 在模式语法分类没有先收敛前，不适合直接冻结这条诊断测试。

### 2. inference 直接命名级回归

- 已尝试为 `INFERRED_TYPE_VARIABLE_INTO_*` 与 `TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR` 构造 focused 样例。
- 当前仓颉 first-party 语义已确认没有官方类型参数注解入口依据，因此 `TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR` 保留为后期扩展模型，但当前不参与检查。
- 这意味着当前 inference 直接命名级回归只继续推进 `INFERRED_TYPE_VARIABLE_INTO_*`、`NEW_INFERENCE_ERROR`、`BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION` 等仍有官方语义依据的项。

## 下一步建议

- 优先继续 `call` / `pattern` 里“已有稳定 producer，但缺 focused regression”的项。
- 如果继续推进 `NOT_OVERLOAD_IN_MATCH`，应先单独整理 `match case pattern` 的语法分类与 CFIR 建模策略。
