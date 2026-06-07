# cfir/semantics/ — CFIR 语义工具

承载 CFIR 上的纯语义查询与工具函数：声明继承关系判断、表达式可写性、数据流辅助、语义相关的诊断构造等。**不**承载 Phase 推进逻辑（那是 `:cfir:resolve` 的职责）。

历史上与 `:cfir:providers` 一起从已弃用的 `:cfir:symbols` 拆分而来。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.declarations` | 声明级语义辅助（继承链、override 关系、可见性判断） |
| `cfir.expressions` | 表达式级语义辅助 |
| `cfir.resolve` | 解析期可复用的语义查询 |
| `cfir.resolve.dfa` | 数据流分析支持 |
| `cfir.diagnostic` | 语义诊断模型 |

## 调用方

- `:cfir:resolve` — Phase 推进中的语义判断
- `:cfir:checkers` — 检查器中的语义查询
- `:analysis:analysis-api-cfir` — IDE 分析后端

## 依赖

- `:cfir:cfir-tree`、`:cfir:cfir-cones`、`:cfir:cfir-common`
- `:common`、`:common:diagnostics`

## 命令

```bash
./gradlew :cfir:semantics:assemble
./gradlew :cfir:semantics:test
```

