# analysis/analysis-api-standalone/ — Standalone 模式 Analysis API

对齐 Kotlin `analysis/analysis-api-standalone`。
为 **非 IDE** 场景（CLI 工具、CI 检查、命令行分析器）提供 Analysis API 的完整实现，不依赖 IntelliJ Application 进程。

## 关键包

`org.cangnova.cangjie.analysis.api.standalone.*` — Standalone session 装配、模块结构构造、平台契约 standalone 实现。

## 使用场景

- 命令行分析器
- 静态检查 CI
- 编译器内嵌分析（如 `--analyze-only`）
- 工件由 `:prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-standalone-for-ide` 聚合

## testFixtures

提供 standalone 模式测试用基础设施。

## 依赖

- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`
- `:analysis:analysis-api-impl-base`、`:analysis:analysis-api-cfir`
- `:cfir:entrypoint`

## 命令

```bash
./gradlew :analysis:analysis-api-standalone:assemble
./gradlew :analysis:analysis-api-standalone:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
- `../../docs/k2-module-alignment.md`
