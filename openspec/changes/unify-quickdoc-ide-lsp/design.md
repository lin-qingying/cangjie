# 快速文档统一：一份内容模型 + 可插拔渲染器

> 基线：当前检出（2026-11-12）。行号、用例数、消费点均经两轮核对。

## 1. 现状

### 1.1 三份渲染器

| 实现 | 位置 | 状态 |
|---|---|---|
| **现役（主仓）** | `code-insight/highlighting/src/org/cangnova/cangjie/lexer/cdoc/render/CDocRenderer.kt` | IDE Quick Doc 正文与行内文档都导入它（`CangJieDocumentationTarget.kt:69`、`CangJieDocumentationTargetProviders.kt:45`）。用 `CjPsiBundle` 的 `kdoc.section.title.*`，无 `@receiver` 分区 |
| **半死（IDE 仓库）** | `intellij-ide/.../highlighter/CDocRenderer.kt` | 仅 `createHighlightingManager`、`highlight`、`appendHighlighted` 现役（`:209`、`:211`、`:435`）。`renderCDoc:373`、`appendCDocSections:387`、`appendSamplesList:635` 等章节渲染**零调用** |
| **半死（deveco 仓库）** | `deveco/modules/ide/base/.../highlighter/CDocRenderer.kt` | 与 IDE 版同源（含 receiver 分区与 `cdoc.*` 键），同样零调用章节渲染 |

### 1.2 LSP

- 入口 `CangjieTextDocumentService.kt:196-205`；实现 `AnalysisApiCangjieAnalysisFacade.kt:559-585`
- 符号恢复走 `toPublicSymbol` / `restoreCallableSymbol`（`:1155-1179`、`:1186+`），不是简单 `element.symbol`
- CDoc 渲染 `renderToDocumentationString:1468` / `collectRenderableTags:1491` / `renderTagLine:1509`，标签平铺，无章节
- 取文档用 PSI `CjDeclaration.findCDoc()`（`:1460-1461`），非 symbol 版
- 回归基线只有 `CangjieLanguageServerTest.kt:32` 的 `hoverProvider` 存在性断言

### 1.3 可复用基础设施

| 能力 | 位置 |
|---|---|
| CDoc PSI / 11 个标签（无 receiver） | `psi/.../cdoc/**`、`CDocKnownTag.kt:32-44` |
| 章节标题 11 个（中英各 11） | `CjPsiBundle.properties:31-41`、`_zh_CN.properties:31-41` |
| CDoc 链接引用 | `analysis/cj-references/.../CDocReference.kt:14`（构造需 `CDocName`，`CaCfirCDocReference` 为 `internal`） |
| 声明渲染 | `CaRenderer.kt:17-25` |
| 现役渲染回归 | `CDocRendererTest.kt`：**5 个 `@Test`、6 组断言** |

### 1.4 关键事实

- IDE Quick Doc 用例 **17 个**（14 + 3，JUnit3 风格 `fun testXxx`）。
- IDE 定义头用 `CaDeclarationRendererForSource.WITH_SHORT_NAMES`（`CangJieIdeDeclarationRenderer.kt:79`）；Analysis 默认是 `WITH_QUALIFIED_NAMES`（`CaRenderer.kt:18`）。两端定义头**当前不同源**。
- `CangJieBundle_zh_CN.properties` 无 `cdoc.section.title` 命中；IDE 的 12 个 `cdoc.section.title.*` 只有英文。
- `findCDocByPsi` 在 `intellij-ide/.../cDocUtil.kt:19`，依赖 `:util` 的 `toLowerCaseAsciiOnly`；`psi` 对 `:util` 是 `implementation`，不传递。
- LSP 未加载 `psiReferenceProvider` EP、未注册 `DumbService`。
- `intellij-ide/`、`deveco/` 是被主仓 `.gitignore:42/:44` 忽略的**独立 Git 仓库**；主仓 `check` 不覆盖它们（根 `build.gradle.kts:374-376`）。
- 现役 `renderCDoc` 的 3 处调用分属两个文件：`CangJieDocumentationTarget.kt:399`、`:404`、`Providers.kt:85`。
- HINT（`computeDocumentationHint`）当前输出**定义头 + 容器行**，无 CDoc 正文、无 ordinal。

## 2. 缺口

1. **格式写死**：共享 HTML 渲染器直接产 `DocumentationMarkup`，LSP 无法复用，只能另写平铺逻辑。
2. **解析未下沉**：宏候选裁决、二级构造器回落、绑定模式、函数类型参数、ordinal 全在 IDE 外壳；LSP 要同源必须复刻。
3. **三份渲染器**：IDE 与 deveco 各带一份零调用的章节渲染副本。
4. **定义头与文档查找语义不一致**：命名策略、modality 显示、`findCDoc` 入口三处不同，属正确性问题。

## 3. 目标架构

原则：**契约只描述内容；格式由渲染器决定；解析与分派只写一次；宿主只接线。**

```
code-insight:highlighting
├── documentation/api          纯数据 + 输出 SPI（零 psi / 零 IntelliJ / 零 analysis）
│     DocumentationContent / DocumentationBody / DocumentationSection(sealed)
│     DocumentationRenderer<R>
│
├── documentation/cdoc         CDoc → sections（依赖 psi，纯函数）
│     buildDocumentationModel(descriptor, typeParameterNames): List<DocumentationSection>
│
├── documentation/resolve      元素 → 符号 → 内容（依赖 psi、analysis-api、cj-references）
│     DocumentationService / DefaultDocumentationService / MacroCandidateResolver
│     SymbolRecovery（搬运 toPublicSymbol / restoreCallableSymbol 的调用约定）
│
├── documentation/html         内容 → Quick Doc HTML（依赖 markdown、平台 HTML 工具）
│     HtmlDocumentationRenderer（render / renderBody）
│     DefinitionRenderer（定义头渲染协作者）
│     DocumentationLinkSupport（宿主注入的超链接与示例源码解析）
│
└── documentation/markdown     内容 → Markdown（依赖 psi 的 bundle，不依赖编辑器/配色）

宿主
├── intellij-ide：target/provider/hint 接线 + IdeDefinitionRenderer + IdeLinkSupport
├── lsp：hover 接线 + MarkdownDocumentationRenderer
└── deveco：随 intellij-ide 同步（独立仓库、独立提交）
```

每层一句话：`api` 定「有什么」；`cdoc` 定「仓颉怎么取」；`resolve` 定「给谁取」；`html`/`markdown` 定「怎么显示」。

### 3.1 为什么不新建模块

`code-insight/README.md:7/:10/:18` 规定「一个用户可见能力 = 一个模块」「契约/机制用包分层」「highlighting 负责 CDoc 渲染」；`:lsp` 与 IDE 都已依赖该模块。新建模块会引入 `settings.gradle.kts`、两份 `prepare` 制品、`deveco` 手工 jar 行与跨构建 testFixtures 问题，收益为零。

## 4. 接口

```kotlin
package org.cangnova.cangjie.documentation.api

/** 唯一输出扩展点。新增宿主/格式只实现此接口。 */
fun interface DocumentationRenderer<R> {
    fun render(content: DocumentationContent): R
}

data class DocumentationContent(
    /**
     * 规范定义头纯文本，由 resolve 层使用共享 Quick Doc renderer 生成：短类型名、有效 modality、统一可见性呈现、local 参数与 Unit 返回类型策略。
     * Markdown 默认直接使用；IDE 只对该规范文本应用语法高亮，不重新计算签名。
     */
    val definition: String,
    /** 仅 HINT 为 null。完整文档目标没有 CDoc 时使用 DocumentationBody(emptyList())。 */
    val body: DocumentationBody?,
    val container: DocumentationContainer?,
)

data class DocumentationBody(
    val sections: List<DocumentationSection>,
    /** CDoc 默认主节的摘要正文；primaryTag 是标签章节时为 null。 */
    val description: String? = null,
    /** 原生 enum 条目序号；HINT 下必须为 null。 */
    val enumOrdinal: Int? = null,
)

sealed interface DocumentationContainer {
    val fqName: String
    data class Package(override val fqName: String, val fileName: String?) : DocumentationContainer
    data class ClassLike(override val fqName: String) : DocumentationContainer
}

// ---------- 章节：类型即种类，不存在 kind 与子类型不一致的状态 ----------

sealed interface DocumentationSection

sealed interface NamedEntrySection : DocumentationSection {
    val entries: List<NamedEntry>
}

data class ParametersSection(override val entries: List<NamedEntry>) : NamedEntrySection
data class TypeParametersSection(override val entries: List<NamedEntry>) : NamedEntrySection
data class PropertiesSection(override val entries: List<NamedEntry>) : NamedEntrySection
/** @throws 与 @exception 合并，顺序保持现役。 */
data class ThrowsSection(override val entries: List<NamedEntry>) : NamedEntrySection

sealed interface TextSection : DocumentationSection {
    val markdown: String
}

data class ConstructorSection(override val markdown: String) : TextSection
data class ReturnsSection(override val markdown: String) : TextSection
data class SinceSection(override val markdown: String) : TextSection
data class SuppressSection(override val markdown: String) : TextSection

data class AuthorsSection(val names: List<String>) : DocumentationSection
data class SeeAlsoSection(val entries: List<LinkEntry>) : DocumentationSection
data class SamplesSection(val entries: List<SampleEntry>) : DocumentationSection

data class NamedEntry(val name: String, val markdown: String, val link: DocumentationLink?)
data class LinkEntry(val label: String, val markdown: String, val link: DocumentationLink?)
data class SampleEntry(val label: String, val link: DocumentationLink?)

/** 只持字符串；不持 PSI，解析由宿主注入（见 D5）。 */
data class DocumentationLink(val label: String, val fqName: String?)
```

```kotlin
package org.cangnova.cangjie.documentation.cdoc

/**
 * 纯函数：只做 CDoc → 章节。
 * 返回空列表表示"有 CDoc 但没有可渲染内容"；"没有 CDoc"由 resolve 层用
 * findCDoc() == null 判断，不在此表达。
 */
fun buildDocumentationModel(
    descriptor: CDocCommentDescriptor,
    typeParameterNames: Set<String>,
): List<DocumentationSection>

/** 提取默认主节的摘要正文；标签主节不重复映射为摘要。 */
fun buildDocumentationDescription(descriptor: CDocCommentDescriptor): String?
```

```kotlin
package org.cangnova.cangjie.documentation.resolve

enum class DocumentationMode { FULL, HINT }

data class DocumentationRequest(
    /** 文档解析目标；可以是声明、引用或宏导航目标。 */
    val element: PsiElement,
    /** 可空的光标源 PSI；宏生成声明必须携带其调用点 PSI。 */
    val origin: PsiElement?,
    /** 可空的 use-site 仓颉源文件；跨文件符号恢复必须提供，不得从目标声明推断。 */
    val useSiteFile: CjFile?,
    val mode: DocumentationMode,
)

sealed interface DocumentationResolution {
    /** 元素不是文档目标，或引用/宏候选无法解析。 */
    data object NotTarget : DocumentationResolution
    /** 外部声明需要跨文件恢复，但请求没有携带 use-site 文件。 */
    data object MissingUseSiteContext : DocumentationResolution
    /** 已识别文档目标，但公开符号恢复失败；不得隐藏为 NotTarget。 */
    data object SymbolRecoveryFailed : DocumentationResolution
    /** 目标解析成功；完整无 CDoc 使用空 body，HINT 使用 null body。 */
    data class Resolved(val content: DocumentationContent) : DocumentationResolution
}

/**
 * 唯一解析入口。
 * NotTarget = 该元素不是文档目标（修饰符、无引用、宏解析失败）。
 * MissingUseSiteContext = 跨文件恢复缺少 use-site 文件。
 * Resolved(body == null) = 是目标但只出签名（HINT）。
 */
interface DocumentationService {
    context(_: CaSession)
    fun resolve(request: DocumentationRequest): DocumentationResolution
}

/** 无状态实现，object 暴露，不注册平台服务（LSP 不加载本模块插件 XML）。 */
object DefaultDocumentationService : DocumentationService
```

```kotlin
package org.cangnova.cangjie.documentation.html

/** 定义头渲染：显式协作者，输入是完整内容，不是无参闭包。 */
fun interface DefinitionRenderer {
    fun render(content: DocumentationContent): String
}

/** 宿主注入：超链接与 @sample 源码提取。返回 null 表示降级纯文本。 */
interface DocumentationLinkSupport {
    fun hyperlink(link: DocumentationLink, label: String): String?
    fun sampleCode(link: DocumentationLink): String?
    /** 可选宿主图标；返回 null 时使用 IntelliJ 通用节点图标。 */
    fun containerIcon(container: DocumentationContainer): String? = null
    fun fileIconSource(): String? = null
}

class HtmlDocumentationRenderer(
    private val project: Project,
    private val definitionRenderer: DefinitionRenderer = DefaultDefinitionRenderer,
    private val linkSupport: DocumentationLinkSupport? = null,
) : DocumentationRenderer<String> {
    override fun render(content: DocumentationContent): String

    /** 仅供 renderCDoc 桥接：只产正文 + 章节，不含定义头与容器。 */
    fun renderBody(body: DocumentationBody): String
}
```

```kotlin
package org.cangnova.cangjie.documentation.markdown

/** 标题表可注入，便于测试固定英文，避免受全局 locale 影响。 */
interface SectionTitles { fun title(section: DocumentationSection): String }

class MarkdownDocumentationRenderer(
    private val titles: SectionTitles = BundleSectionTitles,
) : DocumentationRenderer<String>
```

### 4.1 宿主接线

IDE：

```kotlin
analyze(element) {
    val useSiteFile = originalElement?.containingFile as? CjFile
    when (val resolution = DefaultDocumentationService.resolve(
        DocumentationRequest(
            element = element,
            origin = originalElement,
            useSiteFile = useSiteFile,
            mode = mode,
        ),
    )) {
        is DocumentationResolution.Resolved -> resolution.content
        DocumentationResolution.NotTarget -> return@analyze null
        DocumentationResolution.MissingUseSiteContext -> return@analyze null
        DocumentationResolution.SymbolRecoveryFailed -> return@analyze null
    }
}?.let { content ->
    HtmlDocumentationRenderer(
        project = project,
        definitionRenderer = IdeDefinitionRenderer(project, element), // 高亮 + effectiveModality
        linkSupport = IdeLinkSupport(element),
    ).render(content)
}
```

LSP：

```kotlin
semanticSupport.analyzeSnapshot(document) { snapshot ->
    val target = semanticSupport.findPrimaryTarget(...) ?: return@analyzeSnapshot null
    val origin = semanticSupport.findSemanticLeaf(document, snapshot.psiFile, params.position)
    val resolution = DefaultDocumentationService.resolve(
        DocumentationRequest(
            element = target,
            origin = origin,
            useSiteFile = snapshot.psiFile,
            mode = DocumentationMode.FULL,
        ),
    )
    val content = when (resolution) {
        is DocumentationResolution.Resolved -> resolution.content
        DocumentationResolution.NotTarget -> return@analyzeSnapshot null
        DocumentationResolution.MissingUseSiteContext -> return@analyzeSnapshot null
        DocumentationResolution.SymbolRecoveryFailed -> return@analyzeSnapshot null
    }
    Hover().apply {
        contents = Either.forRight(MarkupContent(MarkupKind.MARKDOWN, MarkdownDocumentationRenderer().render(content)))
        range = semanticSupport.hoverRange(document, snapshot.psiFile, params.position)
    }
}
```

### 4.2 语义边界

- `DocumentationResolution.NotTarget` 与 `Resolved(content.body == null)` 分别表示"不是目标"和"目标但仅签名（HINT）"；跨文件恢复缺上下文和公开符号恢复失败分别使用 `MissingUseSiteContext`、`SymbolRecoveryFailed`。完整文档目标没有 CDoc 时返回 `Resolved`，且 `body = DocumentationBody(emptyList())`，继续输出签名与容器。宿主不得混用。
- 每次解析都必须显式携带可空的光标源 PSI 与可空 use-site `CjFile`。LSP 从同一快照取得 use-site 文件；IDE 只从平台传入的 `originalElement` 取得，禁止从 `element`（可能是库导航目标）推断。直接声明目标在没有 use-site 时只可读取自身公开 symbol；宏生成目标缺少调用点 PSI 时返回 `NotTarget`；跨文件符号恢复缺少 use-site 文件时返回 `MissingUseSiteContext`，有上下文但公开符号仍未恢复时返回 `SymbolRecoveryFailed`。
- callable 声明位于 `CjExtend` 类体时，恢复端按扩展声明包查询 `CaExtendSymbol`，用稳定 `extendId` 匹配扩展，再通过 `declaredMemberScope` 和 callable PSI 等价性恢复成员；普通 class-like 成员仍按 `ClassId` 查询其声明成员作用域，顶层 callable 仍按包名和名称查询。
- 扩展成员被声明宏包装时，恢复端仅跨越 `CjMacroInput` / `CjMacroExpression` 透传节点查找所属类体，避免把包装内 callable 当作顶层声明。
- `.cjo` callable PSI 的 CFIR 搜索也按 `CjExtend` 所在包及 `LLExtendStructuralKeys` 恢复容器；compiled `CjExtend` 没有 `ClassId`，不能走普通 class-like 搜索器。
- callable 的 `containingDeclaration` 关系通过组合后的 `extendProvider` 查询，覆盖源码和二进制库扩展；只查源码 `extendIndexStore` 会让库成员在 Quick Doc 容器渲染时失败。
- Quick Doc 类型别名定义头采用仓颉源码关键字 `type`；词法映射覆盖 `KEYWORDS` 与 `SOFT_KEYWORDS`，使 `public` 等软关键字继续复用编辑器的共享颜色 key。
- HINT：`body == null`、`enumOrdinal` 不可达；容器行保留（与现役 `computeDocumentationHint` 一致）。是否渲染容器由渲染器决定。
- 取消与异常：`render` 不得吞 `ProcessCanceledException` / `CancellationException`；`resolve` 对 PSI 失效、引用解析失败、宏候选歧义返回 `NotTarget`，对外部符号恢复失败返回 `SymbolRecoveryFailed`。共享层是唯一边界，宿主不再各写 try/catch（IDE 的 `catchDocumentationFailure` 降级为日志包装）。

## 5. 决策

### D1 模块落点：`code-insight:highlighting` 内新增包族
见 §3.1。不新增 Gradle 模块，不改 `settings.gradle.kts` / `prepare` / `deveco` 构建列表。

### D2 输出用泛型 SPI
`DocumentationRenderer<R>` 是唯一扩展点。IDE 与 LSP 都取 `String`，第三宿主可取自身 DOM 类型；渲染器的构造依赖由实现类自行声明，契约不承诺无参构造。

### D3 章节用类型表达种类，不设 `kind`
11 个子类型覆盖 11 个 CDoc 标签的渲染结果：`@param` 按名称是否属于类型参数拆成 `ParametersSection` / `TypeParametersSection`，`@throws` 与 `@exception` 合并为 `ThrowsSection`，其余一一对应。不存在 `ReturnsSection` 里塞返回值以外内容的写法。`when` 按家族分支（4 个 `NamedEntrySection` + 4 个 `TextSection` + 3 个独立），新增同形状标签只加一个子类与一行 mapper，新增异形状标签会被编译器逼着补两个渲染器。

### D4 定义头用 `DefinitionRenderer`，并统一策略
`definitionOverride: (() -> String)?` 与内容无关，渲染器实例复用时可能拿到上一个符号的定义头，且无法测试。改为显式 `DefinitionRenderer`，入参是 `DocumentationContent`。resolve 层使用 analysis-api 中共享的 Quick Doc 定义头策略，以短类型名、有效 modality、统一可见性呈现、local 参数及 Unit 返回类型生成规范 `content.definition`。该策略只依据符号与源码语义，不接收消费方标识，也不按平台分支。通用 declaration renderer 与 `PrettyPrinter` 在生成文本的同一轮收集 `CangJieSemanticKind` 区间；共享内容携带纯文本及该注解。HTML renderer 对语义区间应用颜色，对剩余片段应用词法高亮。LSP 只消费纯文本，不承担颜色策略。任何 renderer 都不得重算签名或 modifier。

### D5 链接与 `@sample` 由宿主注入
`CDocReference` 的构造参数是 `CDocName`（`CDocReference.kt:14`），`CaCfirCDocReference` 是 `internal`，无法用 `label/fqName` 重新构造。因此：模型只留 `DocumentationLink(label, fqName)`；IDE 实现 `DocumentationLinkSupport`，用 Analysis 按名称查 PSI 再渲染超链接/取示例源码，并提供 Quick Doc 容器图标；LSP 不注入，降级纯文本、示例省略、容器使用通用图标。共享层不解析，避免在无头环境触发未加载的 EP 与 `DumbService`。

### D6 不引入 `RECEIVER`
`CDocKnownTag.kt:32-44` 只有 11 个标签，无 receiver；仓颉的 receiver 由 `extend` 承载。不新增章节类型、不新增资源键。IDE 与 deveco 副本中的 receiver 分区随死代码删除。

### D7 HINT 语义
仅 HINT 使用 `body == null`，且 `enumOrdinal == null`、容器行保留。完整文档目标没有 CDoc 时返回非空 `DocumentationBody(emptyList())`，与 HINT 可区分。若产品后续要关闭 HINT 容器行，单独改渲染器，不改契约。

### D8 不建跨构建 testFixtures
`intellij-ide` 通过 composite substitution 消费主仓 main 变体，testFixtures 变体不参与替换。改为单一 JSON 期望源 + 两端各自资源副本，并由同步脚本提供复制和校验模式：
- `code-insight/highlighting/testResources/documentation/expectations.json`
- `intellij-ide/modules/ide/base/src/test/resources/documentation/expectations.json`
- `lsp/testResources/documentation/expectations.json` (`lsp` module uses the repository `testResources` source-set directory)

固定使用 `scripts/sync-quickdoc-expectations.ps1`：默认从主仓源复制到两份副本，`-Check` 模式逐字节比较三份文件并在不一致时失败。该同步检查覆盖主仓（highlighting 与 LSP 测试）和 `intellij-ide` 两个 Git 仓库；`deveco` 不消费该 JSON，也不属于该检查。参与期望验收的测试矩阵必须先执行 `pwsh -File scripts/sync-quickdoc-expectations.ps1 -Check`。

### D9 LSP 文档查找入口改为 symbol
统一 `CaDeclarationSymbol.findCDoc()`（含 overridden / expect）。行为变更：重写成员 hover 显示基类描述。加断言并写入改版说明。

### D10 三仓库交付边界
`intellij-ide/`、`deveco/` 是独立 Git 仓库且被主仓忽略。交付拆为：
1. 主仓：`code-insight/highlighting` + `:lsp`；
2. `intellij-ide` 仓库：宿主接线、删死代码、测试；
3. `deveco` 仓库：同步删除同源死代码（可选，不阻塞前两项）。

主仓 `check` 不覆盖 2、3，验收报告分开写。

### D11 语义高亮标注属于通用 renderer 输出
语义类别和 UTF-16 文本区间放在 `common` 的纯数据契约中；Analysis `PrettyPrinter` 与通用 declaration renderer 负责同次渲染采集。Quick Doc 只是该标注契约的一个消费者，不另建 Quick Doc 专用 token enum、字符串扫描器或宿主判断。IDE 对完整定义头执行一次共享 `CangJieHighlighter` 词法扫描：语义区间覆盖的 token 使用语义颜色，其余 token 使用 `CangJieHighlightingLexicalKeys` 的词法颜色；不按语义区间切片后重复调用代码片段高亮器。LSP 保留 Markdown 纯文本。

### D12 modifier 与声明关键字必须由完整字符串渲染
`local` 是修饰符，`let`/`var` 是声明关键字。局部变量 renderer 在 modifier 列表有输出且未以空白结束时补一个分隔符，兼容不同 modifier renderer 的尾随空白约定，并避免重复空格。语义标注只覆盖符号名与类型名，词法高亮器负责关键字颜色。

## 6. 阶段

### 阶段 0：基线
- LSP hover 内容快照（顶层函数、类成员、宏调用、无 CDoc、`@throws`、`@sample`），类名含 `Hover`。
- 记录 `CDocRendererTest` **5 个 `@Test`、6 组断言**；IDE **17 个**用例。
- 锁定现役 HINT 输出（定义头 + 容器行）。

### 阶段 1：契约
`documentation/api`：`DocumentationContent` / `DocumentationBody` / `DocumentationContainer` / 11 个章节子类型 / `DocumentationRenderer`。零平台、零 PSI。源码约束测试。

### 阶段 2：CDoc → 章节
`buildDocumentationModel(descriptor, typeParameterNames)`：章节顺序、类型参数分栏、`@throws`+`@exception` 合并、`@author` 逗号、`@sample`/链接只留标签。`buildDocumentationDescription` 单独保留默认主节正文。表驱动覆盖 11 个子类型与主节摘要。

### 阶段 3：HTML 渲染器
- 先把主仓 `markdownToHtml(CDocTag)` 重构为 `markdownToHtml(text: String, project: Project, ...)`，保留 `CDocTag` 重载；`MarkdownNode` 同样改为持 `text: String`。
- 新增 `HtmlDocumentationRenderer`，`render` 产完整文档，`renderBody` 产正文 + 章节（供 `renderCDoc` 桥接，保持 `CDocRendererTest` 的 `startsWith("<div class='content'>")`）。
- 叶子函数改 `internal` 复用，不复制。
- 出口：`CDocRendererTest` 5 个用例零改动。

### 阶段 4：删除 IDE / deveco 死代码
- IDE：删章节渲染区段（`:373-457`、`:544-705`），保留 `:459-542` 的 `DefinitionRenderer` 支撑（`createHighlightingManager` / `appendHighlighted` / `highlight` / `TextAttributesAdapter`），并删随之无调用的 `highlightQualifiedName:73`、`getTargetLinkElementAttributes:92/123`、`tuneAttributesForLink:106`、`resolveToElement:70`。
- deveco：同源删除，独立提交。
- 判据：`grep` 用显式路径（仓库被 `.gitignore` 忽略，`rg` 默认跳过）。

### 阶段 5：解析下沉 + LSP
- 搬 `computeLocalDocumentation` 全部分支：引用/调用/宏表达式/宏引用/enum constructor/binding pattern/value argument list/`CjLightElement` 回溯/`isModifier`；补二级构造器回落（`CangJieDocumentationTarget.kt:402-406`）。
- 搬 `computeResolvedCallDocumentation` + `matchesTarget` / `matchesNavigationTarget`；搬 `toPublicSymbol` / `restoreCallableSymbol` 的恢复约定。
- 搬 `findCDocByPsi`（依赖 `:util`，显式声明）。
- LSP `hover` 接 `DefaultDocumentationService` + `MarkdownDocumentationRenderer`；删 `renderToDocumentationString` / `collectRenderableTags` / `renderTagLine`。

### 阶段 6：IDE 接线
- `computeDocumentation` / `computeDocumentationHint` 改 service；传 `IdeDefinitionRenderer`（必传，保证 17 个用例的 `WITH_SHORT_NAMES` 输出）与 `IdeLinkSupport`。
- 容器由渲染器输出，IDE 不再拼 `HtmlChunk`。
- 出口：17 个用例零改动；新增 2 个 hint 用例（enum 无 ordinal、含容器行）。

### 阶段 7：一致性验收
- 单一 JSON 期望源 + 三处字节一致的副本（D8）；每条含章节类型序列、章节标题、条目 subject、统一的 `definitionText` 与可选摘要。
- LSP 断言章节标题/类型/subject 与定义头；IDE 断言 `plainText()` 中的相同章节和 subject，并精确包含同一 `definitionText`。
- 出口：两端章节序列、条目 subject 和定义头一致；不登记平台差异策略。

### 阶段 8：统一定义头策略
把 `quickDocModalityModifier:367-387` 对应的 effectiveModality 规则以及共享可见性、local 参数、Unit 返回类型输出策略下沉到 `analysis/analysis-api` 的 Quick Doc renderer policy；默认定义头 renderer 使用该 policy。Analysis policy 只依据符号与源码语义，不包含 LSP/IDE 分支或消费方判断。各高亮 renderer 只消费规范定义头字符串，不再自行裁决签名与 modality。阶段 7 的同一验收 JSON 中，各消费方定义头必须逐字一致，不登记平台差异策略。

### 阶段 9：通用语义注解渲染
- `common` 提供纯数据 `CangJieAnnotatedText`、语义类别与 UTF-16 区间。
- `PrettyPrinter` 和 `CaDeclarationRenderer` 能在一次渲染中产出规范文本与语义区间；普通字符串渲染 API 保持可用。
- Quick Doc 共享服务携带这些通用区间；IDE HTML 适配器按类别上色，并对未标注文本继续使用仓颉词法高亮；LSP 输出不变。
- Analysis 根据符号与类型语义标注，不感知 IDE、LSP 或主题颜色。

### 阶段 10：恢复 Quick Doc 词法颜色与空白
- IDE 定义头 renderer 使用 `CangJieHighlighter` 对整段签名进行一次扫描，逐 token 叠加通用语义颜色；空白 token 原样输出。
- 修正通用局部变量 renderer 与 modifier renderer 的边界，`local let flag: UInt` 必须保留一个空格，形参 modifier 不能因此出现双空格。
- 验证 `let` 等关键字使用编辑器同源词法 key，签名可见纯文本与 `content.definition` 完全一致。

## 7. 验证

| 层 | 命令 | 工作目录 | 判据 |
|---|---|---|---|
| 契约 | `gradlew-queue.bat :code-insight:highlighting:test --tests "*DocumentationApiPurityTest*"` | 主仓 | 零平台 import |
| 模型 | `... --tests "*DocumentationModelTest*"` | 主仓 | 11 个子类型全覆盖 |
| HTML | `... --tests "*CDocRendererTest*"` | 主仓 | 5 个用例零改动 |
| LSP | `gradlew-queue.bat :lsp:test --tests "*Hover*"` | 主仓 | 章节标题 + D9 断言 |
| IDE | `gradlew.bat :modules:ide:base:test --tests "*QuickDocumentation*"` | `intellij-ide/`（可能需 `-Pcangjie.upstream.root=`） | 17 个用例零改动 |

主仓 `check` 不覆盖 IDE / deveco；报告分开写。

## 8. 非目标

- 不新增文档源（注解/外链/Java 互操作）；新增源需改 `resolve` 层，本变更不提供 `DocumentationSource` SPI。
- 不引入 `@receiver`。
- 不让模型持 PSI、`Project`、`TextAttributes`。
- HTML 与 Markdown 的排版仍由各自 renderer 决定；定义头文本与 modality 策略由共享 Analysis renderer 统一。
- 不改 `CaCDocProvider` 提取语义（D9 入口切换除外）。
- 不迁移容器信息的产品策略：`DocumentationContainer` 只提供数据，展示由渲染器决定。

## 9. 审查处置（第二轮）

| 意见 | 处置 | 理由 |
|---|---|---|
| `CDocRendererTest` 是 5 个 `@Test` 不是 6 个 | 采纳 | 全文改为"5 用例 / 6 组断言" |
| IDE 定义头用 `WITH_SHORT_NAMES`，两端不同源 | 采纳 | 删"文本同源"表述，改 `DefinitionRenderer` 显式协作者 |
| `definitionOverride` 闭包是坏味道 | 采纳 | 改 `DefinitionRenderer` 接口，入参 `DocumentationContent` |
| `DocSectionKind` 与子类型双轨可表达非法状态 | 采纳 | 删 `kind`，类型即种类 |
| `DocumentationSubject` 无消费者 | 采纳删除 | YAGNI；ordinal 已隐含 enum 分支 |
| `SampleEntry.code` 是死字段 | 采纳删除 | 源码提取归 `DocumentationLinkSupport` |
| `buildDocumentationModel` 返回 null 与签名矛盾 | 采纳 | 返回 `List<DocumentationSection>`，空列表=无可渲染内容 |
| `DocumentationModel` 单字段包装、`DocBuildContext` 单字段 | 采纳删除 | 直接返回列表、直接传 `typeParameterNames` |
| `Doc*` 与 PSI `CDoc*` 撞名 | 采纳 | 统一 `Documentation*` 前缀 |
| `markdownToHtml` 绑 `CDocTag`，只改可见性不够 | 采纳 | 阶段 3 增加"重构为 String + 保留重载" |
| `CDocReference` 需 `CDocName`，无法由 fqName 重解析 | 采纳 | 改宿主注入 `DocumentationLinkSupport` |
| HINT 含容器行 | 采纳 | D7 明确保留 |
| 二级构造器回落、`toPublicSymbol`、函数类型参数、绑定模式遗漏 | 采纳 | 阶段 5 补全 |
| deveco 有第三份实现；三仓库交付 | 采纳 | D10 + 阶段 4/6 分仓 |
| 新增文档源需要 SPI | 驳回 | 当前无第二来源；改为非目标明确写出，不留假接口 |
| `DocumentationRenderer<R>` 泛型无第二实例 | 部分驳回 | 保留 `R`，但 spec 明确"构造依赖由实现类声明"，不承诺无参构造 |
| 共享 JSON 只安排 LSP 侧 | 采纳 | D8 写死三处路径 |
| 行号/用例数/调用点数处漂移 | 采纳 | 已按实测校正 |
