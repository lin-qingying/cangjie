# cfir/ — Cangjie Frontend IR

CFIR 是仓颉前端的中间表示（Cangjie Frontend IR），对齐 Kotlin K2 FIR。本目录是**聚合模块**（自身无源码），下挂多个子模块覆盖 CFIR 的数据模型、构建、语义解析、检查、序列化与编排。

模块清单以 `settings.gradle.kts` 为准。

## 子模块

### 数据模型与基础设施

| 子模块 | 职责 |
|---|---|
| `cfir-common` | `CfirSession` / `CfirModuleData` / `CfirElement`、基础抽象 |
| `cfir-cones` | 类型系统核心（`ConeCangjieType` / `ConeClassLikeType` / `ConePrimitiveType` 等） |
| `cfir-tree` | 生成式 CFIR 节点（声明 / 表达式 / 类型引用 / visitor / transformer），由 `cfir-tree/tree-generator` 生成 |
| `semantics` | CFIR 语义工具 |
| `providers` | 符号 / 扩展点 providers |
| `diagnostic-renderers` | 诊断渲染器 |

### CFIR 构建（阶段 6 CFIR_BUILD）

| 子模块 | 职责 |
|---|---|
| `raw-cfir/raw-cfir-common` | Raw CFIR 构建共享基类与基础抽象 |
| `raw-cfir/psi2cfir` | PSI → Raw CFIR 转换 |
| `raw-cfir/light-tree2cfir` | LightTree → Raw CFIR 转换 |

### CFIR 语义解析（阶段 7 CFIR_RESOLVE）

| 子模块 | 职责 |
|---|---|
| `resolve` | 多 Phase 语义解析引擎（IMPORTS / SUPER_TYPES / TYPES / STATUS / EXTENSIONS / IMPLICIT_TYPES / BODY_RESOLVE / CHECKERS） |
| `checkers` | 诊断检查器框架（Declaration / Expression / Type checkers），含 `checkers-component-generator` |

### 序列化与编排

| 子模块 | 职责 |
|---|---|
| `cfir-serialization` | `.cjo` 反序列化与跨模块符号加载（序列化写入侧仍在补齐） |
| `entrypoint` | CFIR 前端入口（Session 工厂、Pipeline 配置） |

### 测试

| 子模块 | 职责 |
|---|---|
| `analysis-tests` | CFIR 分析测试套件（基于 `:tests:test-infrastructure`） |

## 阶段对应

CFIR 各子模块在前端管线中的位置（见 `../docs/cjfir-compiler-stages.md`）：

```
阶段 6 CFIR_BUILD     → raw-cfir/*
阶段 7 CFIR_RESOLVE   → resolve + checkers (+ providers + semantics)
阶段 4 IMPORT_PACKAGE → cfir-serialization (反序列化)
阶段 10 SAVE_CJO      → cfir-serialization (写入侧待补)
```

## 相关文档

- `../docs/cjfir-compiler-stages.md` — 阶段设计
- `../docs/cfir-body-resolve-constraint-system-design.md` — BODY_RESOLVE 约束系统
- `../docs/cfir-semantic-analysis-maturity-vs-official-2026-04-08.md` — 实现成熟度评估
- `cfir-tree/tree-generator/Readme.md` — CFIR 节点生成器
- `cfir-tree/resolve-rollback-plan.md` — CFIR_RESOLVE 迁移回滚预案
- `analysis-tests/diagnostics-coverage-gap-vs-cpp.md` — 诊断覆盖缺口
