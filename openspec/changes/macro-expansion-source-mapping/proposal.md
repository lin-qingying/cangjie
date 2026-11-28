# 宏展开的源映射契约与首个消费方

## 为什么

`align-cangjie-completion-with-kotlin-k2` 的 11.5 要求完成「宏 Tokens / 普通表达式 / 可靠展开映射」的上下文分流，并覆盖 `.cjo` 无源码与缺 SDK 降级。分流与降级已落地并由用例锁定，但**「可靠展开映射」这一半被决策为不实现**，理由写在那一行里：分析层没有表达它的契约，且 G1–G5 里没有消费方。

2026-10-11 按「先找已预埋接缝」的标准复核，结论要**修正**：

- **映射设施其实已经存在**——`cfir/providers/.../macro/MacroSurface.kt` 的 `MacroSurface.sourceRange`（展开面的源区间）、`MacroCallForest` 把展开 payload 落在 `replaceRange = surface.sourceRange` 的载体机制、以及 `CfirRawMacroSurfaceUsageStore` 按 `sourceRange` 的 start/end offset 记录使用点；
- 真正缺的是两件：①分析层没有把该映射**暴露成只读契约**；②**没有消费方**——而「造契约而不带消费方」正是 11.5 拒绝做的事。

因此本变更把「消费方」一并立项，让契约有存在理由：**补全在宏调用位与展开内容内的候选来源**。这条消费方在 G1–G5 之内（补全），不需要新建产品面。

## 变更内容

1. **只读契约**：分析层新增「按位置取宏展开源映射」的只读查询——给定 `CjElement`（或偏移）返回该位置的展开映射：展开面身份、其 `sourceRange`、以及**可靠性**（可靠 / 不可靠或未解析）。**不暴露展开产物本身**（`MacroSurface*` 不进 CFIR 的硬性边界不变）。
2. **映射实现**：复用既有 `MacroSurface.sourceRange` 与 `MacroCallForest` 的载体关系（含嵌套宏与跨行情形），不可靠/未解析一律返回 `null`。
3. **首个消费方（补全）**：位置在**可靠展开**的内容内时，候选来源限定为「展开自身可见的名字 + 宏调用处作用域」，不再把与展开无关的工作区名字当候选；映射缺失时**行为必须与今天逐字一致**（宏位置不产出无关候选——11.5 已锁的那条）。
4. **验收**：嵌套宏、跨行宏、无展开、不可靠展开四类用例；IDE 与 LSP 两端一致。

## 功能 (Capabilities)

### 新增功能

- `macro-expansion-source-mapping`: 宏展开的源映射只读契约、其实现（复用既有 `sourceRange`/载体关系）与补全侧的首个消费方。

### 修改功能

（无：宏位置的既有分流行为保持不变，本变更只在不放心的方向上收紧候选来源。）

## 影响

- `cfir/providers`（暴露既有的 surface/载体映射；不改 `MacroSurface` 的 CFIR 边界）；
- `analysis/analysis-api(+cfir)`（只读契约与实现）；
- `code-insight/completion/impl-cfir`（消费方）；
- `lsp`（两端一致回归）；
- **宏侧的欠账仍在**：`MacroCallForest` 等代码引用的 Baseline 计划文档不在仓库里，本变更只依赖已实现的行为，不代宏侧补文档。
