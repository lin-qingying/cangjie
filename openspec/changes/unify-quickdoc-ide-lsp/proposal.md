## 为什么

快速文档有两套实现，且共享层只覆盖了 IDE：`code-insight:highlighting` 的 `CDocRenderer` 直接产出 IntelliJ `DocumentationMarkup` HTML，被 IDE 的 Quick Doc 正文与行内文档导入（`CangJieDocumentationTarget.kt:69`、`Providers.kt:45`）；LSP 拿不到，只能在同一 facade 里另写标签平铺逻辑。同时 `intellij-ide` 与 `deveco` 各带一份**章节渲染零调用**的 `CDocRenderer` 副本（含仓颉不存在的 `@receiver` 分区、只有英文的 `cdoc.*` 键）。

由此产生三个真实问题：

1. **格式不可扩展**：内容与 HTML 绑死，任何非 IDE 宿主都要重写。
2. **解析未下沉**：宏候选裁决、二级构造器回落、绑定模式、函数类型参数、符号恢复（`toPublicSymbol`/`restoreCallableSymbol`）都在 IDE/LSP 各自的外壳里；LSP 想同源必须复刻。
3. **语义不一致**：IDE 定义头用 `WITH_SHORT_NAMES` + `quickDocModalityModifier`，LSP 用默认 `WITH_QUALIFIED_NAMES`；LSP 用 PSI `findCDoc()`，IDE 用 symbol 版。这是正确性差异，不是样式差异。

## 变更内容

- **在 `code-insight:highlighting` 内新增 `documentation` 包族**（`api` / `cdoc` / `resolve` / `html` / `markdown`），不新建 Gradle 模块，不改 `settings.gradle.kts`、`prepare` 制品、`deveco` jar 列表。
- **契约与平台解耦**：`documentation/api` 只含 `DocumentationContent` / `DocumentationBody` / `DocumentationContainer` / 11 个 `DocumentationSection` 子类型 / `DocumentationRenderer<R>`，禁止 import `com.intellij.*`、`psi`、`analysis`。
- **类型即种类**：不设 `DocSectionKind`，11 个子类型与 11 个 CDoc 标签一一对应，非法组合不可表达；`when` 按 4 个 `NamedEntrySection` + 4 个 `TextSection` + 3 个独立分支穷举。
- **定义头策略统一**：`DefinitionRenderer`（入参 `DocumentationContent`）替代无参闭包；共享 Analysis API Quick Doc renderer 固定 IDE 现役的短类型名、有效 modality、可见性、local 参数与 Unit 返回类型策略生成规范 `content.definition`。IDE 只对该文本应用主题语法高亮，不重新计算签名；两端定义头文本一致，不再登记策略差异。
- **链接与 `@sample` 由宿主注入**：`DocumentationLink(label, fqName)` 不持 PSI；IDE 实现 `DocumentationLinkSupport` 解析超链接与示例源码，LSP 不注入、降级纯文本。共享层不构造 `CDocReference`（其构造需 `CDocName`，实现在 Analysis 内且 `internal`）。
- **解析下沉**：`DefaultDocumentationService` 搬运元素分派全部分支（引用/调用/宏/绑定/enum/value arguments/轻量元素回溯/修饰符）、二级构造器回落、宏候选裁决、符号恢复约定、`findCDocByPsi`；请求显式携带可空光标源 PSI 与可空 use-site 文件，并用结构化结果区分非目标、上下文缺失、符号恢复失败和成功内容。
- **HINT 保持现役语义**：仅 HINT 使用 `body == null`、无 ordinal、容器行保留；完整文档目标没有 CDoc 时仍返回非空内容和空 body，以继续展示签名与容器。
- **删除两份死代码**：`intellij-ide` 与 `deveco` 的 `highlighter/CDocRenderer.kt` 章节渲染区段与随之无调用的私有叶子；保留 `createHighlightingManager` / `highlight` / `appendHighlighted`。
- **LSP 删除私有实现**：`renderToDocumentationString` / `collectRenderableTags` / `renderTagLine`。
- **不新增资源键**：`CjPsiBundle` 已有 11 个 `kdoc.section.title.*`（中英各 11），无 `@receiver`，无需迁移 IDE 的 12 个英文 `cdoc.*` 死键。

## 功能 (Capabilities)

### 新增功能

- `quickdoc-support`：快速文档的内容契约与渲染 SPI——模型形状、解析入口、定义头协作者、链接/示例注入边界、HINT 语义、跨端一致性与三仓库交付边界。

### 修改功能

无。本工作树 `openspec/specs/` 为空，本次为上述能力建立规格。

## 影响

- 主仓 `code-insight/highlighting`：新增 `documentation/{api,cdoc,resolve,html,markdown}`；`build.gradle.kts` 增 `implementation(project(":analysis:analysis-api"))` 与 `:util`；重构 `cdoc/render/CDocRenderer.kt` 的 markdown 叶子以接受 `String`，并把其余叶子改 `internal`。
- 主仓 `:lsp`：已依赖 `:code-insight:highlighting`，无需新增依赖；`hover` 改为 `DefaultDocumentationService.resolve` + `MarkdownDocumentationRenderer`；新增 hover 快照（含 `@throws`/`@sample`）。
- `intellij-ide` 仓库：`CangJieDocumentationTarget`、`CangJieDocumentationTargetProviders` 接线；新增 `IdeDefinitionRenderer`、`IdeLinkSupport`；删除死代码；17 个既有用例为零改动门槛。
- `deveco` 仓库：同源死代码同步删除（独立提交，不阻塞主仓与 IDE）。
- 主仓 `check` 不覆盖两个宿主仓库（根 `build.gradle.kts:374-376`），验收报告分开写。
- 行为变更（写入改版说明）：LSP hover 变章节化 Markdown；重写成员显示基类/expect 描述（`CaDeclarationSymbol.findCDoc()`）。
- 兼容性：IDE HTML 结构与 17 个用例、`CDocRendererTest` 5 个用例保持零改动。
- 边界：不新增文档源（无 `DocumentationSource` SPI）；不引入 `@receiver`；模型不持 PSI；不改 `CaCDocProvider` 提取语义。
