# 全项目测试约定（Testing Conventions）

[English](TESTING_CONVENTIONS.md)

本约定适用于仓库内所有一方模块（`settings.gradle.kts` 中包含的模块）。
目标：对齐 Kotlin K2 项目的测试组织方式，同时控制维护成本。

## 1. 测试分类（必须先分类再实现）

### A. 框架型测试（必须接入 test-infrastructure）

满足任一条件即属于框架型测试：
- 使用 `testData` 文件驱动（如 `*.cj` 输入）
- 需要 golden 对比（如 `*.txt`、`*.diagnostics.txt`）
- 需要 directive / 多模块测试结构 / facade-handler 流水线
- 需要 all-files-present 类漏测防护

要求：
- 基于 `:tests:test-infrastructure`（`TestFacade`、`AnalysisHandler`、`AbstractCangjieCompilerTest`）
- `testData` 与测试代码按模块归属放置
- 支持 `-Dupdate.test.data=true` 的显式 golden 更新

### B. 单元型测试（允许直接 JUnit）

满足全部条件可使用直接 JUnit（JUnit4/JUnit5）：
- 不依赖 `testData` 与 golden 文件
- 仅验证纯 API 行为、数据结构、局部算法
- 构造成本低、断言稳定、无文件驱动需求

要求：
- 测试应短小、无跨模块耦合
- 优先覆盖边界条件与回归点

## 1.1 Analysis 模块测试分类清单

`analysis` 下新增或修改测试必须先按本清单归类，并先读取 Kotlin 对照路径。直接 JUnit 只允许保留纯模型、纯格式化或二进制头读取测试；凡是需要 PSI、session、project structure、stub/decompiler/builtins 环境的测试必须接入 `AbstractAnalysisApiExecutionTest` 或 `AbstractAnalysisApiBasedTest`。

### 必须使用 Analysis API 测试框架

| Cangjie 测试 | 当前形态 | Kotlin 对照 |
| --- | --- | --- |
| `analysis/analysis-api-cfir/test/**` | `AbstractAnalysisApiExecutionTest` | `external/kotlin/analysis/analysis-api-fir/tests/**`、`external/kotlin/analysis/analysis-api-impl-base/testFixtures/**` |
| `analysis/stubs/test/.../CaStubSourceGoldenTest.kt` | `AbstractAnalysisApiExecutionTest` + source `testData` + `.stubs.txt` golden | `external/kotlin/analysis/stubs/tests/.../SourceStubsTest.kt` |
| `analysis/stubs/test/.../BuiltinsStubsTest.kt` | `AbstractAnalysisApiBasedTest` + builtins provider + `.stubs.txt`/`.decompiled.text.cj` golden | `external/kotlin/analysis/stubs/tests/.../BuiltinsStubsTest.kt` |
| `analysis/stubs/test/.../CaStubCompiledGoldenTest.kt` | `AbstractAnalysisApiBasedTest` + CJO compiled fixture + golden | `external/kotlin/analysis/stubs/testFixtures/.../AbstractCompiledStubsTest.kt`、`CompiledStubsTestEngine.kt` |
| `analysis/stubs/test/.../CaStubCompiledIntegrationTest.kt` | `AbstractAnalysisApiBasedTest` + CJO compiled fixture + project services | `external/kotlin/analysis/stubs/testFixtures/.../AbstractCompiledStubsTest.kt`、`CompiledStubsTestEngine.kt` |
| builtins/decompiled text golden | 由 `BuiltinsStubsTest` 框架链路覆盖，不保留手写 Application 泄漏型直测 | `external/kotlin/analysis/stubs/tests/.../BuiltinsStubsTest.kt`、`external/kotlin/analysis/stubs/tests/.../BuiltinsDecompilerTest.kt` |

### 允许保留直接 JUnit 的纯测试

| Cangjie 测试 | 保留原因 | Kotlin 对照 |
| --- | --- | --- |
| `analysis/light-declarations/test/.../CaLightDeclarationRendererTest.kt` | 只验证 light declaration 纯模型渲染与缓存，不创建 PSI/project/session | Kotlin light classes 的真实 PSI 行为走 `external/kotlin/analysis/symbol-light-classes/tests/**`；纯缓存/模型契约允许独立直测 |
| `analysis/stubs/test/.../CaStubSnapshotAssemblerTest.kt` | 只验证 `CaStubSnapshotAssembler` 数据聚合，不创建 PSI/project/session | Kotlin stubs 框架把聚合输出放在 `external/kotlin/analysis/stubs/testFixtures/.../StubsTestEngine.kt` 的 golden 流程中 |
| `analysis/stubs/test/.../CaStubTreeSummaryExtractorTest.kt` | 只验证手工 stub tree 到 summary 的纯数据提取，不创建 test project/session | Kotlin 对应为 `external/kotlin/analysis/stubs/testFixtures/.../additionalStubInfoExtractor.kt`，在框架 golden 中间接覆盖 |
| `analysis/decompiled/decompiler-to-stubs/test/.../DecompiledFileStubKindsTest.kt` | 只验证 file stub kind 判定与 part name 去重 | Kotlin 对应模型为 `external/kotlin/compiler/psi/psi-impl/src/.../KotlinFileStubKindImpl.kt` 与 `KtFileStubBuilder.kt` |
| `analysis/decompiled/decompiler-to-file-stubs/test/.../CjoBinaryFileReaderTest.kt` | 只验证 `.cjo` 二进制头读取，不创建 PSI/project/session | Kotlin builtins/class-file 读取链路在 decompiler/stubs framework 中覆盖；仓颉 CJO 头读取是本地二进制格式模型 |

禁止新增以下形态的 analysis 直测：
- 手写 `CangJieCoreEnvironment.createForTests`。
- 依赖其他测试先注册 Application/project service。
- 自行注册 decompiler、builtins、standalone、stub service。
- 用本地 directive 替代 Kotlin 已有的 caret/golden/testData 形态。

## 2. Generated 测试类使用规则

仅在以下场景使用 `Generated`（自动扫描 `testData`）：
- `testData` 数量较多或增长快
- 目录层级复杂，容易漏加测试入口
- 已存在稳定生成器与任务接线（例如 tests-gen + `testGenerator(...)`）

不建议为小规模、稳定集合强制引入 `Generated`，避免过度工程化。

## 3. 目录与命名约定

- 源集目录遵循 `projectDefault()`：
  - `main` -> `src` / `resources`
  - `test` -> `test` / `tests` / `testResources`
  - `testFixtures` -> `testFixtures` / `testFixturesResources`
- 测试类名以 `Test` / `TestCase` 结尾
- golden 文件与输入文件同目录、同名不同扩展

## 3.1 源码输入（Content Roots）

- 测试框架生成编译配置时必须写入 `CONTENT_ROOTS`，保证前端可获取源文件列表。
- `CLI_SOURCE_FILE_PATHS` 仅作为兼容入口，测试中不再使用。

## 4. 新增测试的决策顺序（评审基线）

新增测试时按顺序决策：
1. 是否文件驱动（`testData`）？
2. 是否需要 golden 比对？
3. 是否需要 framework 流水线（facade/handler）？
4. 是否存在漏测风险（需要 all-files-present / Generated）？

若 1~3 任一为“是”，默认走框架型测试。

## 5. 迁移策略（存量测试）

- 不做“一刀切”全量迁移
- 新增/修改时按本约定就近收敛
- 对高频变更目录优先迁移到框架型测试
- 纯单元测试保持轻量即可
