# analysis/light-declarations/ — Light Declaration 模型

对齐 Kotlin `analysis/light-classes` 的部分能力。提供轻量级声明视图，**不需要**完整解析声明体即可获得签名级信息（参数、返回类型、修饰符），供 IDE 快速展示、补全候选、外观渲染。

## 关键包

`org.cangnova.cangjie.analysis.light.declarations.*` — Light declaration 模型与构造工具。

## 设计要点

- 纯模型，不持有 session / project
- 与 stub 索引协同：从 stub 构造，避免完整 PSI 解析
- 可对应反编译声明（与 `:analysis:decompiled` 的 `light-declarations-for-decompiled` 协同）

## 测试

`CaLightDeclarationRendererTest` 是允许保留的纯单元测试（只验证渲染与缓存，不创建 PSI / project / session）；
完整 PSI 行为相关的测试走 `AbstractAnalysisApiExecutionTest` / `AbstractAnalysisApiBasedTest`。

详见 `../../TESTING_CONVENTIONS.md` 第 1.1 节。

## 依赖

- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`
- `:cfir:cfir-tree`、`:cfir:cfir-cones`

## 命令

```bash
./gradlew :analysis:light-declarations:assemble
./gradlew :analysis:light-declarations:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
- `../symbol-light-declarations/README.md` — Symbol-based 版本
