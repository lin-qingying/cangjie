# analysis/analysis-tools/ — Analysis 工具集

对齐 Kotlin `analysis/analysis-tools` 的部分能力。承载基于 Analysis API 的命令行工具与诊断/调试工具：dump session 状态、打印 CFIR 树、批量分析项目、生成诊断报表等。

## 关键包

`org.cangnova.cangjie.analysis.tools.*` — 工具入口（main 函数 / CLI 命令）。

## 使用场景

- 开发调试：dump 当前 session 的 CFIR 树 / scope / symbol provider 状态
- 批量分析：在 CI / 离线场景跑 analysis API
- 报表：生成诊断 / 覆盖矩阵 / 模块依赖图

## 依赖

- `:analysis:analysis-api`、`:analysis:analysis-api-standalone`
- `:cfir:entrypoint`

## 命令

```bash
./gradlew :analysis:analysis-tools:assemble
./gradlew :analysis:analysis-tools:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
