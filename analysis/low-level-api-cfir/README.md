# analysis/low-level-api-cfir/ — 低层 Analysis API 的 CFIR 实现

对齐 Kotlin `analysis/low-level-api-fir`。
为 `:analysis:analysis-api-cfir` 提供**更底层**的 CFIR session 管理与按需解析能力：缓存、依赖追踪、Phase 推进入口、声明级懒解析。

`analysis-api-cfir` 是公共能力的实现，`low-level-api-cfir` 是其下层的"按需在 CFIR 上跑解析"的底层引擎。

## 关键包

`org.cangnova.cangjie.analysis.low.level.*` — Low-level session、resolver、cache、project-structure 接入。

## testFixtures

提供低层测试基础（按需解析场景、声明 Phase 跟踪等）。

## 依赖

- `:cfir:entrypoint`、`:cfir:resolve`、`:cfir:checkers`、`:cfir:cfir-tree`、`:cfir:cfir-cones`
- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`、`:analysis:analysis-api-impl-base`
- `:psi`

## 命令

```bash
./gradlew :analysis:low-level-api-cfir:assemble
./gradlew :analysis:low-level-api-cfir:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
- `../../docs/k2-module-alignment.md` — Kotlin `low-level-api-fir` 对照
