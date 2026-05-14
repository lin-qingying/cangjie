# docs/ — 文档索引

本目录收录仓颉前端项目的架构 / 对齐 / 设计 / 评估 / 计划 / 对照类文档。
真相源始终是代码与 `settings.gradle.kts`；本目录的文档用于解释**为什么**这样设计，以及与 Kotlin K2 / 官方 C++ 编译器的**差异与对齐策略**。

## 顶层导览

| 文件 | 角色 | 说明 |
|---|---|---|
| [`current-module-organization.md`](current-module-organization.md) | 模块现状 | 当前 `settings.gradle.kts` 中所有一方模块的分层视图与简要职责 |
| [`module-organization.md`](module-organization.md) | 模块规划 | 12 阶段管线与 K2 对齐目标驱动的模块拆分规划 |
| [`compiler-module-design.md`](compiler-module-design.md) | 模块设计 | 按子系统划分编译器模块的设计基线 |
| [`k2-module-alignment.md`](k2-module-alignment.md) | 模块对照 | 仓颉模块 ↔ Kotlin K2 模块的逐一对照表 |
| [`psi-cfir-ast-chir-alignment.md`](psi-cfir-ast-chir-alignment.md) | 节点对照 | PSI ↔ CFIR ↔ 官方 AST ↔ CHIR 的节点级对照 |

## CFIR / 语义设计

| 文件 | 角色 | 说明 |
|---|---|---|
| [`cfir-body-resolve-constraint-system-design.md`](cfir-body-resolve-constraint-system-design.md) | 设计 | `BODY_RESOLVE` 阶段的约束系统与类型对比系统设计 |
| [`type-inference-four-systems-comparison.md`](type-inference-four-systems-comparison.md) | 对照 | 四套类型推断 / 约束系统对照表 |
| [`cfir-semantic-analysis-gap.md`](cfir-semantic-analysis-gap.md) | 评估快照 | 2026-03-13 时点的语义分析基础设施完备性分析（已被更新版评估覆盖） |
| [`cfir-semantic-analysis-maturity-vs-official-2026-04-08.md`](cfir-semantic-analysis-maturity-vs-official-2026-04-08.md) | 评估快照 | 2026-04-08 CFIR 语义分析相对官方编译器的实现程度评估 |

## 诊断对照

| 文件 | 角色 | 说明 |
|---|---|---|
| [`official-compiler-diagnostics.md`](official-compiler-diagnostics.md) | 对照 | 官方仓颉编译器诊断清单 |
| [`diagnostics-gap-vs-official-cpp-full.md`](diagnostics-gap-vs-official-cpp-full.md) | 对照 | 与官方 C++ 编译器全量诊断 gap |
| [`diagnostics-gap-vs-official-cpp-sema-detailed.md`](diagnostics-gap-vs-official-cpp-sema-detailed.md) | 对照 | 语义诊断详细 gap |
| [`diagnostics-gap-vs-official-cpp-sema-status-2026-04-06.md`](diagnostics-gap-vs-official-cpp-sema-status-2026-04-06.md) | 状态快照 | 2026-04-06 语义诊断补齐主计划完成状态 |

## 语言特性

| 文件 | 角色 | 说明 |
|---|---|---|
| [`cangjie_features.md`](cangjie_features.md) | 参考 | 基于官方文档整理的仓颉语言特性清单 |

## 计划 / Plans

| 文件 | 角色 | 说明 |
|---|---|---|
| [`plan-conflicting-type-constraints.md`](plan-conflicting-type-constraints.md) | 计划 | 冲突类型约束处理计划 |
| [`plan-operator-overload-numeric-widening.md`](plan-operator-overload-numeric-widening.md) | 计划 | 运算符重载数值扩宽计划 |
| [`plans/2026-03-23-resolution-common-inference-migration.md`](plans/2026-03-23-resolution-common-inference-migration.md) | 计划 | `resolution.common` 推断迁移计划 |

## 归档

[`archive/`](archive/) 下保留**已被取代或重复**的历史快照与设计稿，见 [`archive/README.md`](archive/README.md)。

## 阅读建议

- 想了解**当前实际模块**：先看 `current-module-organization.md` 与项目根 `README.md`。
- 想了解**应该如何拆**：先看 `compiler-module-design.md` 与 `module-organization.md`。
- 想了解**与 Kotlin K2 的对应关系**：看 `k2-module-alignment.md`。
- 想了解**前端 / IR 实现进度**：看时间最新的 `cfir-semantic-analysis-maturity-vs-official-*.md` 与 `diagnostics-gap-vs-official-cpp-sema-status-*.md`。
- 想知道某份带日期文档是否已被覆盖：检查 `archive/` 是否已存在新版。
