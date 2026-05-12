# analysis/analysis-test-framework/ — Analysis API 共享测试框架

供 `:analysis:*` 各子模块使用的测试基础设施，对齐 Kotlin `analysis-api-impl-base/testFixtures`。
本模块**只发布 `testFixtures`**，本身无 main 源码。

## 关键包（testFixtures）

`org.cangnova.cangjie.analysis.test.*` — 测试 services、fixtures、handlers、test engine 装配。

## 提供的测试基类

- `AbstractAnalysisApiBasedTest` — 通用分析测试入口（依赖 PSI / session / project / builtins）
- `AbstractAnalysisApiExecutionTest` — 执行类测试入口（component 验证、generated 测试）

## 强制约束

下游模块的 analysis 测试**必须**继承上述基类，不允许：

- 手写 `CangJieCoreEnvironment.createForTests`
- 依赖其他测试先注册 application / project service
- 自行注册 decompiler / builtins / standalone / stub service
- 用本地 directive 替代上游已有的 caret / golden / testData 形态

例外（纯模型 / 二进制头读取 / 渲染缓存）见根 `TESTING_CONVENTIONS.md` 第 1.1 节"允许保留直接 JUnit 的纯测试"。

## 依赖

- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`、`:analysis:analysis-api-impl-base`、`:analysis:analysis-api-cfir`
- `:tests:test-infrastructure`（testFixtures）

## 使用方式

```kotlin
// 下游 build.gradle.kts
dependencies {
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
}
```

## 上游接入

本模块的 testFixtures 由 `:prepare:analysis-test-framework` 聚合为发布工件 `cangjie-frontend-analysis-test-framework`。

## 相关文档

- `../../TESTING_CONVENTIONS.md` 第 1.1 节 — Analysis 模块测试分类清单（强制）
- Kotlin 对照：`external/kotlin/analysis/analysis-api-impl-base/testFixtures/**`
