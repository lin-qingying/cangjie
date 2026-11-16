## 0. 交付边界

本变更跨三个 Git 仓库：主仓、`intellij-ide/`、`deveco/`（后两者被主仓 `.gitignore:42/:44` 忽略）。第 1–4、6 组落主仓；第 5、7 组落 `intellij-ide`（`deveco` 可选）；第 8 组跨仓。各组完成须分别记录改动与验证命令，不合并为"全绿"。

## 1. 基线

依据：[设计](design.md) §1、阶段 0；[规格](specs/quickdoc-support/spec.md)。

- [x] 1.1 为 LSP hover 补内容快照，测试类名含 `Hover`，落点 `lsp/test/org/cangnova/cangjie/lsp/analysis/`。场景：顶层函数（`@param`/`@return`）、含非内建类型的定义头、类成员、宏调用、无 CDoc、`@throws`、`@sample`。判据：当前实现下通过并锁定现状。
- [x] 1.2 记录 `CDocRendererTest` 基线：**5 个 `@Test`（`:86/:114/:134/:161/:180`）、6 组断言**，含 `startsWith("<div class='content'>")`、`Returns:` 早于 `Since:`、`<strong>`/`<em>`、`Params:`/`See Also:`、反引号消费、空文档 null。
- [x] 1.3 记录 IDE 17 个用例：`CangJieQuickDocumentationTest`（14）+ `CangJieEnumQuickDocumentationTest`（3），JUnit3 风格 `fun testXxx`。
- [x] 1.4 锁定现役 HINT 输出：定义头 + **容器行**，无 CDoc 正文、无 ordinal（`CangJieDocumentationTarget.kt:104-107` → `:380` → `:499-505`）。
- [x] 1.5 写入实施记录：主仓版 `renderCDoc` 有 3 处调用、2 个文件（`CangJieDocumentationTarget.kt:399/:404`、`Providers.kt:85`）；IDE 版章节渲染零调用，但 `highlight`/`appendHighlighted` 现役（`:209/:211/:435`）；`deveco` 存在同源副本。

## 2. 契约（documentation/api，主仓）

依据：[设计](design.md) §4、D2、D3。

- [x] 2.1 建 `documentation/api`：`DocumentationContent`、含 `description` 的 `DocumentationBody`、`DocumentationContainer`（sealed：`Package`/`ClassLike`）、`DocumentationRenderer<R>`、11 个 `DocumentationSection` 子类型（4 个 `NamedEntrySection`、4 个 `TextSection`、`AuthorsSection`/`SeeAlsoSection`/`SamplesSection`）、`NamedEntry`/`LinkEntry`/`SampleEntry`/`DocumentationLink`。
- [x] 2.2 不设 `DocSectionKind`，不设 `DocumentationSubject`，不设 `DefinitionPolicy`，不设 `SampleEntry.code`。
- [x] 2.3 源码约束测试 `DocumentationApiPurityTest`：`documentation/api/**` 不得 import `com.intellij.*`、`org.cangnova.cangjie.psi.*`、`org.cangnova.cangjie.analysis.*`。判据：违规时失败并打印文件:行。
- [x] 2.4 `documentation/api` 需 `@OptIn(CjNonPublicApi::class, CjImplementationDetail::class)` 的类型集中标注；契约单测验证渲染器可用 lambda 替换，并覆盖 4 个 `NamedEntrySection` 子类、4 个 `TextSection` 子类及 3 个独立子类。渲染器测试覆盖两个家族 dispatch 分支和三个独立分支。穷举性由 sealed 类型上的无 `else` `when` 在编译期保证；新增同族类型由家族分支自动处理，不要求渲染器改动。

## 3. CDoc → 章节（documentation/cdoc，主仓）

依据：[设计](design.md) §4、D5、D6。

- [x] 3.1 实现 `buildDocumentationModel(descriptor, typeParameterNames): List<DocumentationSection>`，返回空列表表示"有 CDoc 无可渲染内容"；"无 CDoc"不在本函数表达。
- [x] 3.2 章节顺序与过滤对齐主仓版 `CDocRenderer.appendCDocSections:125-176`：params → type params → properties → constructor → return → throws/exception → author → since → suppress → see → sample。
- [x] 3.3 类型参数拆分：`typeParameterNames` 由调用方传入（IDE 用 `CjTypeParameterListOwner`，LSP 用 `CaTypeParameterOwnerSymbol`），不沿父链猜。
- [x] 3.4 `@sample`/链接只产出 `DocumentationLink(label, fqName)`；不解析 PSI。`fqName` 允许为 null（单段名）。
- [x] 3.5 表驱动单测 `DocumentationModelTest`：11 个子类型全覆盖；参数/类型参数分栏不重叠；`@throws`+`@exception` 合并顺序；`@author` 逗号拼接；空内容不产章节。
- [x] 3.6 `buildDocumentationDescription(descriptor)` 保留默认主节摘要；测试覆盖默认主节有内容、无内容与标签主节不重复。

## 4. HTML 渲染器（documentation/html，主仓）

依据：[设计](design.md) §4、§4.2、D4、D5。

- [x] 4.1 重构主仓 `CDocRenderer` 的 markdown 叶子：`markdownToHtml(text: String, project: Project, allowSingleParagraph: Boolean)`、`MarkdownNode(text: String, ...)`，保留 `CDocTag` 重载；`processTableRow`/`getTableAlignment`/`appendHyperlink`/`highlightQualifiedName`/`getTargetLinkElementAttributes`/`trimCommonIndent`/`extractExampleText` 改 `internal`。
- [x] 4.2 新增 `HtmlDocumentationRenderer(project, definitionRenderer, linkSupport)`：`render(content)` 产完整文档（定义头 + 正文 + 容器）；`renderBody(body)` 只产正文 + 章节。
- [x] 4.3 `DefinitionRenderer`（`fun interface`，入参 `DocumentationContent`）与默认实现 `DefaultDefinitionRenderer`（转义原样输出）；不提供无参闭包参数。
- [x] 4.4 `DocumentationLinkSupport` 由宿主注入：超链接与 `@sample` 源码提取；可提供宿主容器图标；返回 null 时降级纯文本/省略示例并用通用图标。共享层不构造 `CDocReference`（其构造需 `CDocName`）。
- [x] 4.5 `CDocRenderer.renderCDoc(contentTag, sections)` 桥接为 `renderBody(...)`，保持三处调用签名不变；`CDocTemplate.DescriptionBodyTemplate.CangJie()` 决定 `startsWith("<div class='content'>")`。
- [x] 4.6 判据：`gradlew-queue.bat :code-insight:highlighting:test --tests "*CDocRendererTest*"` 5 个用例零改动通过。

## 5. 删除死代码（intellij-ide / deveco 仓库）

依据：[设计](design.md) §1.1、阶段 4。

- [x] 5.1 `intellij-ide/.../highlighter/CDocRenderer.kt`：删 `renderCDoc:373-385`、`appendCDocSections:387-440`、`appendAuthors:443-457`、`appendThrows:544-575`、`appendSeeAlso:577-603`、`appendTag:605-611`、`appendTagList:679-705`、`appendSection:673-677`、`appendSamplesList:635-656`、`getTargetElement:613-616`、`appendHyperlink:618-633`、`MarkdownNode:53-68`、`toHtml:154-341`、`markdownToHtml:343-368`、`appendCDocContent:370-371`、`getTableAlignment:127`、`processTableRow:142`、`htmlEscape:658`、`trimCommonIndent:665-671`、`extractExampleText:660-663`、`highlightQualifiedName:73-90`、`getTargetLinkElementAttributes:92/123`、`tuneAttributesForLink:106`、`resolveToElement:70`。
- [x] 5.2 保留 `:459-542`：`TextAttributesAdapter`、`createHighlightingManager`、`appendHighlighted`、`highlight`。
- [x] 5.3 `deveco` 同源副本同步删除，独立提交；不阻塞 5.1。
- [x] 5.4 判据：删除后 `grep` 必须用显式路径（两仓库被 `.gitignore` 忽略，`rg` 默认跳过）；IDE 编译通过，17 个用例通过。

## 6. 解析下沉与 LSP（主仓）

依据：[设计](design.md) §4、§4.2、D9、阶段 5。

- [x] 6.1 `documentation/resolve`：`DocumentationMode`、`DocumentationRequest(element, origin, useSiteFile, mode)`、sealed `DocumentationResolution`（`NotTarget` / `MissingUseSiteContext` / `SymbolRecoveryFailed` / `Resolved`）、`DocumentationService`、`DefaultDocumentationService`（object，无状态，不注册平台服务）。`origin` 与 `useSiteFile` 可空；宏生成目标必须有对应调用点，跨文件符号恢复必须携带 use-site 文件。
- [x] 6.2 搬 `computeLocalDocumentation:145-256` 全部分支：`CjBindingPattern`、`CjMacroExpression`、`CjNameReferenceExpression`、`CjEnumConstructor`、`CjDeclaration`、`CjLightElement`（`cangjieOrigin`）、`CjValueArgumentList`、`CjCallExpression`、`isModifier`；每个成功目标的 `definition` 使用共享 Quick Doc renderer policy 生成。
- [x] 6.3 补二级构造器回落：声明自身无 CDoc 且为 `CjSecondaryConstructor` 时取包含类型的 CDoc（`CangJieDocumentationTarget.kt:402-406`）。
- [x] 6.4 搬宏候选裁决：`computeResolvedCallDocumentation`、`CaCall.toDeclarationSymbol`、`matchesTarget`、`matchesNavigationTarget`（`:305-350`）。宿主不得再写 `when (callInfo)`。
- [x] 6.5 搬符号恢复约定：`toPublicSymbol` / `restoreCallableSymbol`（`AnalysisApiCangjieAnalysisFacade.kt:1155-1179`、`:1186+`），跨文件恢复显式使用 `DocumentationRequest.useSiteFile`，保证有 use-site 请求的库/工作区声明可恢复；直接声明目标无 use-site 时只读取自身公开 symbol，不从目标文件推断 use-site。缺少跨文件上下文返回 `MissingUseSiteContext`；公开 symbol 恢复失败返回 `SymbolRecoveryFailed`。为直接声明、缺少 use-site、符号恢复失败分别增加 service 测试；补 LSP 库符号 hover 用例，并断言恢复使用 hover 文档所在文件的 use-site 模块。
- [x] 6.6 搬 `findCDocByPsi`（`intellij-ide/.../cDocUtil.kt:19`）到 `documentation/cdoc`，显式加 `implementation(project(":util"))`（`psi` 对 `:util` 是 `implementation`，不传递）。
- [x] 6.7 HINT：仅 HINT 使用 `body == null`、`enumOrdinal == null`、容器保留（D7）。完整文档目标无 CDoc 时返回非空内容及 `DocumentationBody(emptyList())`；为该情况增加 service 与 LSP 快照断言。
- [x] 6.8 LSP `hover` 从快照取得可空光标源 PSI 与非空 `snapshot.psiFile`，构造 `DocumentationRequest` 后接 `DefaultDocumentationService` + `MarkdownDocumentationRenderer`；对 `NotTarget`、`MissingUseSiteContext` 和 `SymbolRecoveryFailed` 分别显式处理，不得因直接声明目标的 `origin` 为空而跳过 hover。删 `renderToDocumentationString:1468`、`collectRenderableTags:1491`、`renderTagLine:1509`；`documentationOf` 若因此无调用一并删除。
- [x] 6.9 `MarkdownDocumentationRenderer(titles: SectionTitles)`：定义头围栏代码块；章节 `## <title>`；`NamedEntry` 输出 `- \`name\` — markdown`；`SamplesSection` 只输出 label（无源码）；`DocumentationLink` 不做伪链接。`SectionTitles` 默认实现读 `CjPsiBundle`，测试注入固定英文。
- [x] 6.10 更新 1.1 快照：断言 `## Params` / `## Returns` / `## Throws` / `## Samples`；重写成员显示基类描述（D9）。

## 7. IDE 接线（intellij-ide 仓库）

依据：[设计](design.md) §4.1、D4、D10、阶段 6。

- [x] 7.1 `computeDocumentation` / `computeDocumentationHint` 构造 `DocumentationRequest` 后改接 `DefaultDocumentationService.resolve` + `HtmlDocumentationRenderer`；传递平台给出的 `originalElement` 作为可空 `origin`，仅从其 `containingFile` 提取可空 `useSiteFile`，不得用目标 `element` 的文件补造 use-site 上下文；显式处理 `NotTarget`、`MissingUseSiteContext` 与 `SymbolRecoveryFailed`。
- [x] 7.2 `IdeDefinitionRenderer` **必须传入**：用 `createHighlightingManager` 对共享 `content.definition` 做语法高亮；纯文本不得重算，与 LSP 文本一致。
- [x] 7.3 `IdeLinkSupport`：按 `DocumentationLink.fqName`/`label` 经 Analysis 查 PSI，渲染超链接与 `@sample` 源码；解析失败降级。
- [x] 7.4 容器由渲染器输出；IDE 删除 `getContainerInfo:410-469` 的 HTML 拼接，仅提供 `DocumentationContainer` 数据（图标仍用 `CangJieQuickDocumentationIcons`）。
- [x] 7.5 判据：17 个用例零改动通过；新增 2 个 hint 用例（enum 无 ordinal、含容器行）。

## 8. 一致性验收（跨仓）

依据：[设计](design.md) §7、D8。

- [x] 8.1 单一期望源 `code-insight/highlighting/testResources/documentation/expectations.json`，字段：用例名、章节子类型名序列、章节标题、条目 subject、统一的 `definitionText`、可选摘要。显式复制到 `lsp/testResources/documentation/` 与 `intellij-ide/modules/ide/base/src/test/resources/documentation/`；新增 `scripts/sync-quickdoc-expectations.ps1`，默认同步，`-Check` 逐字节校验并在副本不一致时失败。
- [x] 8.2 LSP 断言章节标题、章节类型序列、条目 subject 与 `definitionText`；IDE 断言 `plainText()` 含相同标题、章节类型序列、条目 subject 与 `definitionText`。
- [x] 8.3 IDE 与 LSP 定义头都按同一 JSON 的 `definitionText` 精确断言；不允许平台差异策略。
- [x] 8.4 不建跨构建 `testFixtures`（理由见 D8）。
- [x] 8.5 `code-insight/README.md` 补 `documentation` 包族说明（不改模块清单）。
- [x] 8.6 执行矩阵前先在主仓运行 `pwsh -File scripts/sync-quickdoc-expectations.ps1 -Check`；再执行主仓 `:code-insight:highlighting:test`、`:lsp:test --tests "*Hover*"`；`intellij-ide` 目录下 `:modules:ide:base:test --tests "*QuickDocumentation*"`（worktree 场景加 `-Pcangjie.upstream.root=`）。

## 9. 后续独立变更：定义头语义统一（不在本次交付）

- [x] 9.1 把 `quickDocModalityModifier:367-387` 下沉到 `analysis/analysis-api` 的共享 modifier 渲染策略。
- [x] 9.2 默认定义头 renderer 采用共享 Quick Doc policy：`WITH_SHORT_NAMES`、effectiveModality、统一可见性呈现、local 参数与 Unit 返回类型策略；policy 只依据符号语义，不按消费方分支。
- [x] 9.3 删除 `IdeDefinitionRenderer` 的定义头策略分支与本次登记的差异策略；确保 analysis-api policy 无 LSP/IDE 消费方判断，并同步更新设计 D4/§4.2 与 Q6/Q9 期望。
- [x] 10.1 在 `common` 增加平台无关的仓颉语义文本契约：语义类别、UTF-16 半开区间与 `CangJieAnnotatedText`，验证区间范围有序且不重叠。
- [x] 10.2 将语义区间采集提升到通用 `PrettyPrinter` / `CaDeclarationRenderer` 输出，并提供 Analysis renderer 的 annotated output 接口；普通文本 render API 继续复用同一 renderer。
- [x] 10.3 `DocumentationContent` 和共享解析服务承载通用标注，不添加 Quick Doc 专用类别映射或平台逻辑。
- [x] 10.4 IDE HTML 定义 renderer 按通用语义类别应用既有颜色 key；LSP Markdown 仍只输出规范纯文本。
- [x] 10.5 增加服务回归断言：函数名、值参数名、参数与返回类型名有正确类别和准确范围；IDE 文档测试继续断言定义头纯文本与共享期望完全一致。
- [x] 11.1 IDE 定义头 renderer 对完整签名执行一次 `CangJieHighlighter` 扫描，按 semantic range 覆盖 token key；保留每个空白 token，禁止按 span 切片后重复词法化。
- [x] 11.2 修正通用局部变量 renderer 在 modifier 与 `let`/`var` 关键字间的分隔符，并确保形参 modifier 不出现双空格；新增 `local let flag: UInt` 精确文本回归。
- [x] 11.3 IDE 回归验证 `let` 关键字使用编辑器共享 lexer/color key，且渲染纯文本与 `DocumentationContent.definition` 完全一致。

## 12. 跨文件扩展成员符号恢复

- [x] 12.1 `DocumentationService.restoreCallableSymbol` 按 `CjExtend` 所在包查询 `CaExtendSymbol`，从其 `declaredMemberScope` 中按 PSI 身份恢复成员；保留 class-like 与顶层 callable 的既有路径。
- [x] 12.2 增加跨文件扩展函数 Quick Doc 回归，断言 `UInt64.fromLittleEndian` 形态的扩展成员恢复成功并保留 CDoc 摘要；IDE 定向测试 6 个用例零失败。

## 13. 完整定义头词法颜色与扩展宏包装恢复

- [x] 13.1 共享词法 key 映射覆盖 `CjTokens.KEYWORDALL`，让 `public` 等软关键字与普通关键字一样使用编辑器高亮 key。
- [x] 13.2 Analysis 类型别名 renderer 输出仓颉源码 `type`；IDE 回归断言 `public`、`type` 与类型别名名称均有样式，且纯文本等于共享定义头。
- [x] 13.3 callable owner 查找只越过声明宏的 `CjMacroInput` / `CjMacroExpression` 透传节点；IDE 回归覆盖 `@Identity` 包装的扩展方法。

## 14. compiled PSI 扩展成员的 CFIR 搜索

- [x] 14.1 `CfirDeclarationForCompiledElementSearcher` 对 `CjExtend` 直接声明与其 callable 成员按包和 `LLExtendStructuralKeys` 找到 CFIR 容器，不再调用要求 `ClassId` 的 class-like 搜索。
- [x] 14.2 使用真实 `std.binary.cjo` 的 `UInt64.readLittleEndian` Quick Doc 回归，验证参数签名渲染期间 compiled callable/parameter symbol 可恢复，并产生 IDE HTML。
- [x] 14.3 `CaCfirSymbolRelationProvider` 通过组合后的 `extendProvider` 恢复源码和库扩展成员 owner，保证容器行构造不触发 library-origin PSI 异常。
