# docs/archive/ — 历史归档

本目录保留**已被取代或重复**的历史文档快照。每份文档**当时是有效的**，但已被更新版本或合并版本取代。保留用途：

- 追溯设计 / 评估 / 实现状态在某个时间点的形态；
- 为变更回溯提供线索；
- 避免误把过期模块名 / 路径当作"现状"引用。

| 文件 | 归档原因 | 取代者 |
|---|---|---|
| `cfir-implementation-status-2026-03-14.md` | 早期连续每日快照，已被后续版本覆盖 | `cfir-implementation-status-2026-03-19.md` 与 `cfir-semantic-analysis-maturity-vs-official-2026-04-08.md` |
| `cfir-implementation-status-2026-03-15.md` | 同上 | 同上 |
| `cfir-implementation-status-2026-03-17.md` | 同上 | 同上 |
| `cfir-implementation-status-2026-03-19.md` | 2026-03-19 全量盘点，已被更晚的成熟度评估覆盖 | `cfir-semantic-analysis-maturity-vs-official-2026-04-08.md` |
| `cfir-constraint-system-gap-2026-03-19.md` | 约束系统 gap 时点快照，已纳入 BODY_RESOLVE 约束系统设计 | `cfir-body-resolve-constraint-system-design.md` |
| `cfir-gap-analysis-vs-official-2026-03-20.md` | 与官方 gap 分析时点快照 | `cfir-semantic-analysis-maturity-vs-official-2026-04-08.md` |
| `diagnostics-progress-2026-04-06-enum-constructor-and-blockers.md` | enum constructor 一个子领域的进度记录 | `diagnostics-gap-vs-official-cpp-sema-status-2026-04-06.md` |
| `compiler-module-design1.md` | 与 `compiler-module-design.md` 内容近重复的旧版 | `../compiler-module-design.md`（顶层版本是更新后的"V2"） |

## 阅读注意

- 归档文档中提到的模块名可能已过期（如 `:cfir:symbols` 当前已演化为 `:cfir:semantics` + `:cfir:providers`，`:cfir:diagnostics` 实际为 `:common:diagnostics` + `:cfir:diagnostic-renderers`，`:cfir:deserialization` 已并入 `:cfir:cfir-serialization`）。
- 当前实际模块清单以 `settings.gradle.kts` 与 [`../current-module-organization.md`](../current-module-organization.md) 为准。
