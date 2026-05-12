# analysis/analysis-api-impl-base/ — Analysis API 实现基础层

对齐 Kotlin `analysis/analysis-api-impl-base`。
为各后端（`analysis-api-cfir` / `analysis-api-standalone`）提供**与平台无关**的共享实现基类与工具：组件骨架、生命周期辅助、缓存、模型工厂等。

## 关键包

`org.cangnova.cangjie.analysis.api.impl.*` — 后端实现共享层。

## testFixtures

提供后端测试共用的辅助类（与 `:analysis:analysis-test-framework` 协作）。

## 调用方

- `:analysis:analysis-api-cfir`、`:analysis:analysis-api-standalone`、`:analysis:low-level-api-cfir`

## 依赖

- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`

## 命令

```bash
./gradlew :analysis:analysis-api-impl-base:assemble
./gradlew :analysis:analysis-api-impl-base:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
- `../../TESTING_CONVENTIONS.md` 第 1.1 节
