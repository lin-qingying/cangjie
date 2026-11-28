# mainS5 F7/F8/F9：现有 6 项测试 API 编译（2026-11-19）

现有 `CjoSourcePublicPayloadTest.kt` **API 编译成功，退出码 0**。`javap` 确认生成类包含全部 6 个测试方法。测试源码与正确期望均未修改；未运行 Gradle/cjc；未执行 JUnit，用例运行数仍为 0。

## 实际编译输入

唯一树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。

- 使用 main 已生成的真实 first-party classes，包括更新后的 semantics.api、cfir-tree、cfir-serialization。
- 依赖来自已有 serialization test worker classpath `build/cjmp-s5-public-payload-20261119/api-compile-result.json` 所记录的完整路径；过滤已存在的正式 main/testFixtures 输出和 jar，新增模块输出显式置前。
- 直接调用 Kotlin JVM compiler 2.2.0，JDK 21/JVM target 21 与现行仓库工具链一致。
- `-Xjvm-default=all`、`-XXLanguage:+ExplicitBackingFields`、`-Xskip-prerelease-check`、`-Xcontext-parameters` 原样取自 `repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/common-configuration.gradle.kts:21–25`。没有为测试放宽 API、删除断言或修改生产代码。
- 输出目录独立于模块的正常 classes：`build/cjmp-s5-public-payload-20261119/api-classes`。

实际命令（在唯一树执行）：

```powershell
& 'C:/Users/lin17/.jdks/corretto-21.0.10/bin/java.exe' '@build/cjmp-s5-public-payload-20261119/compile-java.args'
```

编译器只报告仓库显式启用 `ExplicitBackingFields` 的内部特性/预发布提示；无测试源码错误、无缺失 API。

## 编译结果

| 生成方法 | API 编译 |
| --- | --- |
| `publicHeaderPreservesBodylessFunctionAndRealHelper(Path)` | 成功 |
| `publicGraphKeepsHeadersAcrossHeaderFirstAndBodyFirstQueries(Path)` | 成功 |
| `headerOnlyQueriesNeverDecodeAnAvailableButInvalidBody(Path)` | 成功 |
| `fourStringKindsSurviveThePublicGraph(Path)` | 成功 |
| `eachMatchPatternBorrowsItsOwnSelectorAndKeepsItsPayload(Path)` | 成功 |
| `aRepeatedCaseIdentityWithDifferentSelectorsIsRejected(Path)` | 成功 |

源码 SHA-256：`B06B3E13793591ADEB3991FFC3A6C6F491AC59C9201D8283F6D356D3989F0E03`，与首轮交付相同。

生成测试类 SHA-256：`43E2ABF9A5EAB565F4C26909DE21EF95A59A4A6DFADBAF39FBC5528A242E5DA5`；生成类 61524 字节。机器证据：[api-compile-result.json](../../build/cjmp-s5-public-payload-20261119/api-compile-result.json)。

## 当前 API 状态与运行边界

本轮重读确认 provider.sourceHeaderOf、正式 header cache、Graph.functionHeaders、header.bodyAvailable、Literal.stringKind 的四值 encoder/decoder 映射及 Pattern.contextExpression 已可被现有测试源码消费。SourceGraphLoader 对 body0 只采用已登记的真实函数；首轮报告中“body0 被扫描无条件跳过”的观察已不适用，测试仍保留原完整 header 断言。

该结果只验证现有 6 项测试的源码/公共 API 编译，不验证四字符串、两个 selector 回边、坏正文隔离等运行断言，也不覆盖主线程下一步的 source 原 literal stringKind 与正式 stage 验证。

原 [测试语义报告](main-s5-f7-f8-f9-test-semantics-writer-20261119.md) 中的真实输入缺口仍存在：现有 SDK 1.0.5 core 资源含 String、缺 JString。本轮资源 SHA 仍为 `E02D58B8AF3BA450860C6D14EDD37A927E54DA3C88C037E0C86DFD7BF10351AD`；编译不会执行 `stringMetadata`，不以“API 编译成功”宣称该运行前置条件已经解决。
