# dependencies/intellij-core/ — IntelliJ 平台核心依赖聚合

把 IntelliJ Platform 的核心制品（util / core / lang / analysis / indexing / project-model / concurrency / extensions 等）通过 `api` 聚合成一方模块，其他模块统一以 `intellijCore()`（`compileOnly`）引用，避免各自直连平台坐标与版本。对齐 Kotlin `dependencies/intellij-core`。本模块不含源码，仅作为平台依赖边界。

## 聚合的平台制品

通过 `api` 聚合，版本统一取 `intellijSdkVersion`（当前 `253.29346.379`）：

| 制品 | 职责 |
|---|---|
| `util` / `util-base` / `util-rt` / `util-class-loader` / `util-xml-dom` | 基础工具与运行时 |
| `plugins-parser-impl` | 插件 XML 解析模型（`RawPluginDescriptor` / `PluginDescriptorReaderContext` / `PluginDescriptorBuilder` 等） |
| `core` / `core-impl` | 平台核心 |
| `ide-impl` / `concurrency` | 线程/运行时（stub-backed PSI 测试需要，保持传递以拉取 rwmutex-idea、concurrency 等） |
| `extensions` | 扩展点 |
| `lang` / `lang-impl` | 语言支持（`ParserDefinition`、`Language`、`FileType`、`PsiBuilder` 等） |
| `analysis` / `analysis-impl` / `indexing` / `indexing-impl` | 分析与索引 |
| `project-model` / `project-model-impl` | 项目模型 |

另 `runtimeOnly(libs.opentelemetry.api)`：平台运行时只保留一份 OpenTelemetry API，供 analysis-api 统计后端（`CangJieGlobalOpenTelemetryProvider`）在 IDE 运行时解析；不参与编译传递。

## 依赖

- 无一方模块依赖（刻意保持为纯依赖边界）。
- 被 `:psi`、`:lsp`、`:code-insight:*`、`:cfir:raw-cfir:light-tree2cfir`、`prepare:frontend-embeddable`、`analysis` 等模块以 `intellijCore()` 统一引用（约定见 `repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/intellijDependencies.kt`）。

## 命令

```bash
./gradlew :dependencies:intellij-core:assemble
```

## 相关文档

- `../../README.md` — 仓库总览
- `../../docs/module-catalog.md` — 模块目录
