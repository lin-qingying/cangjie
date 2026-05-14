# analysis/analysis-api-cfir/ — Analysis API 的 CFIR 实现后端

把 `:analysis:analysis-api` 暴露的抽象绑定到具体的 CFIR 实现。对齐 Kotlin `analysis/analysis-api-fir`。

## 关键包

`org.cangnova.cangjie.analysis.api.cfir.*` — CFIR 后端 components 实现，按 component 类型分包（resolve / type / call / scope / completion / diagnostics 等）。

## 子模块

`analysis-api-cfir-generator` — 生成器，按 Kotlin analysis-api-fir-generator 风格产出样板代码。

## 依赖

- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`
- `:analysis:analysis-api-impl-base`
- `:analysis:low-level-api-cfir`
- `:cfir:entrypoint`、`:cfir:resolve`、`:cfir:checkers`、`:cfir:cfir-tree`、`:cfir:cfir-cones`、`:cfir:cfir-common`
- `:psi`

## 测试

测试**必须**接入 `AbstractAnalysisApiExecutionTest` / `AbstractAnalysisApiBasedTest`，严禁手写 `CangJieCoreEnvironment.createForTests` 或自行注册 application service / decompiler / builtins。详见 `../../TESTING_CONVENTIONS.md` 第 1.1 节。

测试 fixture 位于 `testFixtures/`，由 `:analysis:analysis-test-framework` 提供共享基础设施。

```bash
./gradlew :analysis:analysis-api-cfir:test
```

测试套件由 `tests-gen/` 下生成的 `Generated` 测试类驱动，扫描 testData 目录：

- `cases/generated/cases/components/diagnosticProvider/Cfir*CollectDiagnosticsTestGenerated.java` 等

## 上游接入

本模块由 `:prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-cfir-for-ide` 与 `-module` 形态聚合为 IDE 插件依赖工件。

## 相关文档

- `../README.md` — Analysis 模块总览
- `../../TESTING_CONVENTIONS.md` — 测试约定
- `../../docs/k2-module-alignment.md` — Kotlin K2 对照
