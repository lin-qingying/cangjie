## 为什么

当前 CFIR 前端在测试与 CLI 管线中通过自定义的 `CLI_SOURCE_FILE_PATHS` 获取源文件，导致配置分散、与 Kotlin 编译器生态不一致，并在测试环境中出现“源文件为空→CHECKERS 无输入”的系统性问题。现在对齐 Kotlin 的配置与收集方式，能从根本上统一来源、降低维护成本，并为后续模块化/多源输入打下基础。

## 变更内容

- 采用与 Kotlin 一致的“内容根(Content Roots)”作为源码输入的唯一入口，建立统一的 source roots 配置与收集流程。
- 在 CFIR 前端管线中替换自定义 `CLI_SOURCE_FILE_PATHS` 读取逻辑，改为读取并解析 `CONTENT_ROOTS`（含 KotlinSourceRoot/JavaSourceRoot 等类型）。
- 在测试基础设施中统一写入 `CONTENT_ROOTS`，确保所有测试具备一致且可追踪的源文件来源。
- 明确弃用 `CLI_SOURCE_FILE_PATHS`（如需保留，提供兼容层与迁移路径，而不是双轨长期并行）。

## 功能 (Capabilities)

### 新增功能
- `cfir-content-roots-input`: 统一的源文件输入能力，使用 `CONTENT_ROOTS` 描述源码与依赖根，并通过统一的收集流程生成 CFIR 输入。
- `cfir-source-collection-alignment`: 与 Kotlin 的 source roots 收集行为对齐（含重复检测、平台/公共源拆分与扩展点）。

### 修改功能
（无）

## 影响

- 受影响模块：`compiler/cli`、`cfir/entrypoint`、`tests/test-infrastructure`、`cfir/analysis-tests` 等涉及配置与源文件收集的模块。
- 受影响 API/配置：`CompilerConfiguration` 中的源文件输入键位；现有 `CLI_SOURCE_FILE_PATHS` 将被弃用或迁移。
- 运行方式：测试与 CLI 前端输入路径将统一来源，可能需要同步更新相关配置与文档。
