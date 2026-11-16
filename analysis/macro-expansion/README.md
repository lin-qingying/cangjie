# analysis/macro-expansion/ — 宏语义展开的宿主无关编排

宏语义展开的宿主无关编排：宿主环境 SPI、每工程编排服务与会话触发点（Analysis 层一等能力）。宿主（IDE / LSP / standalone / 测试）只需注册一份环境 SPI 实现，"何时构造、何时发布、何时失效"的决策全部在本模块；未注册环境时宏展开完全不触发，行为与未接入宏展开的宿主一致。

引擎 SPI（`MacroSemanticExpansionEngine` 及 `SdkHandle` / `ModuleRoots` / `MacroExpansionRevision` 等取值类型）位于 `macro:macro-common`，Kotlin 侧无宏机制，故无对应 Kotlin 模块。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.analysis.macro.expansion` | `MacroSemanticExpansionEnvironment`（宿主环境 SPI：SDK / 源码根与 classpath 根 / 输入 revision / 读访问 / 发布屏障与回调）；`MacroSemanticExpansionService`（每工程编排服务：会话触发入口、in-flight 去重、模块级串行化、revision 与失败记录、`awaitIdle`）；`MacroSemanticExpansionConstruction`（构造流程：pre-macro 整模块构建、无宏短路、artifact 准备、符号索引、import 绑定、展开、屏障内发布与输入稳定性重试）；`MacroSemanticSessionConfigurator`（触发点，注册到 `llCfirSessionConfigurator` 扩展点） |

## 依赖

- `:analysis:low-level-api-cfir`
- `:macro:macro-common`
- `:compiler:config`（环境 SPI 直接暴露 `CompilerConfiguration`，属公开契约）
- IntelliJ Platform（`compileOnly`）

testFixtures 提供 `FakeMacroSemanticExpansionEnvironment` / `FakeMacroSemanticExpansionEngine`，供本模块与其他 analysis 模块的用例复用。

## 命令

```bash
./gradlew :analysis:macro-expansion:assemble
./gradlew :analysis:macro-expansion:test
```

## 相关文档

- `../../docs/macro-expansion-host-integration.md` — 新宿主接入指南（引擎 SPI / 编排框架 / 宿主环境三层分工）
- `../README.md` — Analysis 模块总览
- `../../TESTING_CONVENTIONS.md` — 测试约定（本模块用例基于 fake 引擎，属允许保留直接 JUnit 的纯模型测试）
