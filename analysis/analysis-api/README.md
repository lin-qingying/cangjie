# analysis/analysis-api/ — 公共分析 API

仓颉 Analysis API 的对外接口层，对齐 Kotlin `analysis/analysis-api`。
为 IDE、standalone 工具与上游消费者提供**与具体后端实现解耦**的稳定接口。

## 关键包

| 包 | 职责 |
|---|---|
| `analysis.api` | API 入口（`KaSession` 等核心抽象） |
| `analysis.api.components` | 按 component 拆分的能力（resolve / type / call / scope / completion / annotations / diagnostics / etc.） |
| `analysis.api.compile` | 编译流程相关入口 |
| `analysis.api.completion` | 补全能力 |
| `analysis.api.dataFlow` | 数据流分析能力 |
| `analysis.api.decompiled` | 反编译入口 |
| `analysis.api.annotations` | 注解读取 |

## 设计原则

- **接口优先**：调用方依赖本模块接口，不接触具体实现
- **生命周期**：所有 API 调用必须在合法 `KaSession` 生命周期内
- **K2 对齐**：包结构、类名尽量与 `external/kotlin/analysis/analysis-api/src/**` 对应

## 实现后端

- `:analysis:analysis-api-impl-base` — 公共实现层
- `:analysis:analysis-api-cfir` — 基于 CFIR 的实现
- `:analysis:analysis-api-standalone` — Standalone 模式
- `:analysis:low-level-api-cfir` — 低层 API 的 CFIR 实现

## 平台抽象

平台契约由 `:analysis:analysis-api-platform-interface` 提供。

## 命令

```bash
./gradlew :analysis:analysis-api:assemble
./gradlew :analysis:analysis-api:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
- `../../TESTING_CONVENTIONS.md` 第 1.1 节 — Analysis 模块测试分类
- `../../docs/k2-module-alignment.md` — 与 Kotlin K2 模块对照
