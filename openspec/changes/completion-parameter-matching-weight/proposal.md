# 补全「参数匹配」权重维度

## 为什么

`align-cangjie-completion-with-kotlin-k2` 的 10.3 要求实现分析期权重与平台 sorter 组合，覆盖来源、import 成本、类型匹配、类别、弃用、同级次序与**参数匹配**七个维度。前六维已落地并各有用例，**只有「参数匹配」没做**，原因写在那一行里：设计全文只在 §7.3 的覆盖清单里提过一次、没有口径，仓内也没有「期望参数」契约。

2026-10-11 复核把这条从「没有口径」推进到「缺什么很明确」：

- 期望类型的契约在 `analysis/analysis-api/.../components/CaExpressionTypeProvider.kt`，实现只有 `analysis-api-cfir` 一处；
- 该实现里**没有**任何从「所在调用表达式的实参位」推导期望参数类型的代码（`CjValueArgument` / `argumentIndex` / `valueArguments` 与期望相关的 grep 无命中）——即 `ExpectedTypeMatch` 覆盖赋值/返回/lambda 等上下文，**实参位不在内**；
- 因此「参数匹配」不是隐式已完成，而是需要**先补一层分析层推导**。

本变更是把这块补上：让位于实参位置的候选，按**该位形参的类型**参与排序。

## 变更内容

1. **分析层推导**：在期望类型推导里补「实参位」分支——位置落在 `CjValueArgument` 内时，取所在调用的被调方符号，按其形参列表取对应位的形参类型作为期望类型；命名实参照形参名匹配。推导不出来时一律返回「未知」，**不得**猜测。
2. **完成侧权重**：把该结论接进既有权重位段的「期望类型匹配」一维（不新增维度、不新增平台 weigher）——即实参位与其它上下文共用同一条语义：**匹配则加权、未知则无影响**。
3. **验收用例**：位置实参匹配/不匹配/未知、命名实参按名匹配、普通表达式位（无调用上下文）行为不变；并沿用既有的极值验算传统（低位维度拿到极值不得越过高位维度）。

## 功能 (Capabilities)

### 新增功能

- `completion-parameter-match-weight`: 实参位的期望参数类型推导与其在补全排序中的权重表达（分析层推导 + 完成侧接入 + 两端一致回归）。

### 修改功能

（无：不改动既有规范级行为，实参位的排序此前没有规范约束。）

## 影响

- `analysis/analysis-api`（若需要在 `CaExpressionTypeProvider` 的契约文档里明确「实参位」这一上下文）；
- `analysis/analysis-api-cfir`（推导实现与其单测）；
- `code-insight/completion/impl-cfir`（权重接入点 `CompletionSortDataImpl` 的既有位段）；
- `lsp`（两端候选一致性的回归：IDE 与 LSP 共用同一份权重）；
- 预算：每次补全多一次类型查询，需进 `code-insight/completion/completion-stage-budget.properties` 的 `INDEX_QUERY`/`CONTEXT` 档观察。
