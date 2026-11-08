# Design: 仓颉 Inlay Hints（v7，已吸收第六轮端到端与惯例审查意见）

状态：六轮审查完成，P0 可开工

架构对齐声明：本方案与仓库既有架构决策一致——completion 提案（`openspec/changes/align-cangjie-completion-with-kotlin-k2/proposal.md`）确立的"**IDE 与 LSP 是同一候选生成实现的两个消费端，LSP 侧只做协议适配**"原则，hints 照此执行（共享 collector + 双端转换）。
参考实现：`external/kotlin-plugin`（K2 IDE provider）、`external/kotlin-lsp`（复用 IDE provider 转 LSP）
同构先例：`code-insight:folding` 的 `CangJieFoldingRangeCollector`（"只产出宿主无关区域；IDE 与 LSP 分别转换协议对象"）

## 0. 现状（第一轮评审已核实）

| 层 | 状态 |
|---|---|
| LSP 协议管线 | 已通：`CangjieLspRequest.INLAY_HINT`（`CangjieLspRequest.kt:67`）、`CangjieServerCapabilitiesFactory.kt:101`、`CangjieClientCapabilityNegotiation.kt:91`、testkit `LspIntegrationTestConnection.kt:283` |
| LSP 语义 | 桩：`AnalysisApiCangjieAnalysisFacade.kt:969-973` 返回 `emptyList()`；`supportedFeatures`（:95-114）未开 `inlayHints` |
| LSP resolve | **缺失**：`CangjieTextDocumentService.kt` 只有 `inlayHint` override（:470-478），无 `resolveInlayHint` |
| IDE 插件 | 无任何 provider；但 `intellij-ide/modules/ide/base/src/main/resources/inlayProviders/` 已预置 preview 资源（见 §5） |
| Analysis API | `analyze(file){}` → `CaSession`（`analyze.kt:27-32`）；`CaExpressionTypeProvider.expressionType/returnType/expectedType`、`CaTypeInformationProvider.isErrorType`、`CaRenderer.render()`、`CaResolver.resolveToCall(): CaCallInfo?`（`CaResolver.kt:41`）、`CaMacroExpansionMappingProvider.macroExpansionMappingAt` 全部可用 |

## 1. 共享层：新建 `:code-insight:hints` 模块

### 1.1 模块接线（v2 补全，共 8 处）

1. 根 `settings.gradle.kts`（:75-85 code-insight 区）：`include(":code-insight:hints")`
2. 根 `settings.gradle.kts`（:39-44 区）：`include(":prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-hints-for-ide")`
3. 根 `settings.gradle.kts`（:55-60 区）：`include(":prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-hints-for-ide-module")`
4. 新建 `code-insight/hints/build.gradle.kts`：照 `code-insight/folding/build.gradle.kts`（`compileOnly(intellijCore())`）；对 `:psi`/`:common` 用 `implementation`（与 folding :13-14 一致）；对 `:analysis:analysis-api` 也用 `implementation`（先例 `code-insight/api/build.gradle.kts:13`，评审 N5：folding 实际是 implementation 而非 api）
5. 新建 `prepare/ide-plugin-dependencies/cangjie-frontend-code-insight-hints-for-ide/build.gradle.kts`：照 folding-for-ide，**只打 hints 自身 jar**（`:code-insight:hints`），不重复打 `:analysis:analysis-api`/`:psi`（IDE 类路径已有 `cangjie-frontend-analysis-api-for-ide` 等，评审 A4）
6. 新建 `prepare/ide-plugin-dependencies-module/...-hints-for-ide-module/build.gradle.kts`：照 folding-module 版本，含 `exclude(group = "com.jetbrains.intellij.platform")`
7. `intellij-ide/settings.gradle.kts`（:69-76 substitution 区）：新增 `substitute(module("org.cangnova.cangjie:cangjie-frontend-code-insight-hints-for-ide"))`
8. `intellij-ide/gradle/libs.versions.toml`（:98 附近）：新增 `cangjieCodeInsightHintsForIde` 条目；`intellij-ide/modules/ide/base/build.gradle.kts`（:21-32）compileOnly + testImplementation 各加一行

### 1.2 数据模型

```kotlin
// code-insight/hints/src/org/cangnova/cangjie/codeinsight/hints/
enum class CangJieInlayHintKind { TYPE, PARAMETER_NAME, OTHER }

data class CangJieInlayHintLabelPart(
    val text: String,
    val target: SmartPsiElementPointer<PsiElement>? = null,  // 仅 IDE 填；LSP 侧丢弃（评审 E2）
)

data class CangJieInlayHint(
    val offset: Int,
    val paddingLeft: Boolean,
    val paddingRight: Boolean,
    val kind: CangJieInlayHintKind,
    val label: List<CangJieInlayHintLabelPart>,   // 全部为已渲染纯文本
    val tooltip: String? = null,                  // 已渲染纯文本
)

data class CangJieInlayHintOptions(
    val showPropertyTypes: Boolean = true,
    val showLocalVariableTypes: Boolean = true,
    val showFunctionReturnTypes: Boolean = true,
    val showParameterNames: Boolean = true,
)
```

### 1.3 生命周期硬约束（评审 E1，必须遵守）

`CaSession` KDoc 禁止泄漏到 `analyze` 块外。因此：
- **collector 内部必须在 `analyze` 块内把所有 label/tooltip 渲染成 `String`**，`CangJieInlayHint` 不得持有 `CaSession` / `CaSymbol` / `CaType` / 任何 lifetime owner；
- `target` 只允许 `SmartPsiElementPointer`（PSI 层对象，不属于 session 生命周期）；
- collector 自身无状态、不缓存 session。

### 1.4 收集器接口

```kotlin
object CangJieInlayHintCollector {
    /**
     * 单一 range 入口。调用方负责提供处于活跃生命周期的 CaSession
     * （本函数不调用 analyze，仅在块内被调用）。
     */
    fun collect(
        file: CjFile,
        range: TextRange?,                    // null = 全文件；LSP 传请求 range
        session: CaSession,
        options: CangJieInlayHintOptions,
    ): List<CangJieInlayHint>
}
```

内部按收集器分组，逐项独立实现（对应 options 开关）。**P0 仅实现 `CjTypeHintsCollector`；`CjParameterNameHintsCollector` 为 P1 加入，勿在 P0 过度实现**（评审 N9）：
- `CjTypeHintsCollector`：`CjProperty`（含局部）、`CjNamedFunction` 返回类型——**仅当源码无类型引用时**显示；类型查询 `expressionType`/`returnType`；渲染显式传 `CaTypeRendererForSource.WITH_SHORT_NAMES`（`CaRenderer.render()` 默认是全限定名，评审 B3）
- `CjParameterNameHintsCollector`：`CjCallExpression` → `resolveToCall(): CaCallInfo?` → 实参绑定形参名；预留 exclude-list 扩展点

### 1.5 过滤规则（v2 修正）

- 错误过滤：子树含 `CjErrorElement`（`psi/src/.../CjErrorElement.kt`）或 `type.isErrorType`（`CaTypeInformationProvider.kt:20`）→ 不产 hint（**不存在 `CjErrorExpression`，评审 F1**；`CfirErrorExpression` 属 cfir 层，PSI 层碰不到）
- 宏区域：用现成 API `session.macroExpansionMappingAt(file, offset)` / `PsiElement.macroExpansionMapping`（`CaMacroExpansionMappingProvider.kt:26-33`）判定，不自建（评审 F2）
- range 外元素、`isDefault` project、非 `CjFile` 均跳过
- 单元素异常吞掉记日志，不影响其余 hint（对标 K2 `collectInlaysWithErrorsLogging`）

## 2. LSP 接入（P0）

`lsp/src/org/cangnova/cangjie/lsp/analysis/AnalysisApiCangjieAnalysisFacade.kt`：

1. `supportedFeatures` 加 `inlayHints = true`（:95-114 处一行）。注意：`CangjieLspFeatureSet.frameworkDefaults`（:107）**已置 true**，且 `enabledFeatures = descriptor.features ∩ supportedFeatures`（`CangjieServerContext.kt:87`）——facade 一行即同时让真实 server 与 testkit 生效，勿重复添加（评审 N6）
2. 实装 `inlayHints()`（替换 :969-973 桩），照 `foldingRanges()`（:905-922）模板：
   `semanticSupport.analyzeSnapshot(document) { snapshot -> CangJieInlayHintCollector.collect(snapshot.psiFile, range, this, LSP_OPTIONS).map { 转换 } }`
   （`analyzeSnapshot` 的 lambda receiver 即 `CaSession`，实参写 `this`，评审 N7；`snapshot.psiFile: CjFile` 见 `AnalysisApiLspSemanticSupport.kt:84`）
   遵循 facade 头注释（:57-63）的 `document -> snapshot -> session` 主链；**照 :307-329 惯例接取消检查**（`context.cancellation.throwIfCancelledRecording` + `ProcessCanceledException` 处理，第六轮 D13）
3. 新建 `lsp/.../hints/CangjieLspInlayHintConverter.kt`：
   - offset→Position 用 `LspTextDocument.analysisPositionAt`（`LspTextDocument.kt:78`）
   - kind：TYPE→`InlayHintKind.Type`，PARAMETER_NAME→`Parameter`
   - **label 一律纯 `String`**（lsp4j 1.0.0 的 `InlayHintCapabilities` 无 `labelKindSupport` 字段，评审 F3）；converter 丢弃 target
   - P0 不填 data（见 §2.1）
4. `CangjieServerCapabilitiesFactory.kt:101`：`setInlayHintProvider(true)` 改为 `setInlayHintProvider(InlayHintRegistrationOptions().apply { resolveProvider = true })`（lsp4j 1.0.0 中字段类型为 `Either<Boolean, InlayHintRegistrationOptions>`，**类名是 `InlayHintRegistrationOptions` 而非 `InlayHintOptions`**，评审 N1；`resolveProvider` 字段已核实存在）——前提是 §2.1 的 resolve override 已就位
5. options：LSP 侧硬编码全开（`LSP_OPTIONS = CangJieInlayHintOptions()`），共享层 options 参数仅 IDE 消费；后续演进走 `CangjieLanguageServerDescriptor` 级 flag（`useLegacyCompletion` 先例，facade :226）或 `didChangeConfiguration`，不走 optionId 映射（评审 E3）

### 2.1 resolve 兜底与会话存储（评审 C3/E2/N2，P0 必须含）

- **P0**：resolve 兜底是**三层链路**（第四轮 D4 修正，缺一不可）：
  1. `CangjieLspRequest` 新增 `RESOLVE_INLAY_HINT("inlayHint/resolve")` 枚举项
  2. `CangjieAnalysisFacade` 接口新增 `fun resolveInlayHint(context: CangjieAnalysisRequestContext, hint: InlayHint): InlayHint`（:319-323 `inlayHints` 旁）
  3. `AbstractCangjieAnalysisFacade` 加默认 override `resolveInlayHint(...) = hint`——**禁止照抄 ：254-258 的 `unsupported()` stub 模式**（unsupported 会抛异常，兜底语义是原样返回）。子类 `AnalysisApiCangjieAnalysisFacade` 与 `ProtocolContractAnalysisFacade`（:32，均继承 Abstract）自动继承兜底，**testkit 零改动**；P3 时 AnalysisApi 再 override 实装
  4. `CangjieTextDocumentService` override `resolveInlayHint(hint)` 经 `requestExecutor.compute(CangjieLspRequest.RESOLVE_INLAY_HINT)` 调 facade（照 `resolveCompletionItem` 模式，:183-189）。lsp4j 1.0.0 `TextDocumentService` 的 default 实现会抛 `UnsupportedOperationException`，广告 `resolveProvider=true` 前必须先有 override
  5. **P0 同步项（第三轮 dry-run 新发现）**：`CangjieLspRequestCoverageTest.lspMethodsUseProtocolNamespace`（`lsp/test/.../CangjieLspRequestCoverageTest.kt:64-78`）的命名空间白名单为 `setOf("textDocument", "workspace", "completionItem", "codeAction")`，**不含 `inlayHint`**——新增枚举必须同步在 ：65 白名单加 `"inlayHint"`，否则该测试失败。`CangjieRequestStatistics`（:51/:63/:72）用 `entries.associateWith` 自动覆盖新枚举，无需改动
- **P3（resolve 实装）**：不用 SmartPsiElementPointer 跨请求传递（LSP 快照在 didChange/didClose 经 `upsertSnapshot` 重建，pointer 失效）。采用仓库既有会话存储模式（completion 的 `lifecycle.completionSessions` / `LspCompletionSessionStore`（`CangjieServerContext.kt:43`）先例，facade :330/:391-429）：
  - `inlayHints()` 响应后把 `{uri, version, offset, kind, index}` → 本次 hint 的 label/tooltip 缓存进 lifecycle 会话存储
  - hint.data 存上述纯文本 JSON；`resolveInlayHint` 按 key 查缓存，未命中则重新 `analyze` 按 offset 重查并返回
  - P3 前置检查：协商层读客户端 `InlayHintCapabilities.resolveSupport.properties` 决定是否广告 `resolveProvider=true`（`CangjieClientCapabilityNegotiation.kt:91` 目前仅 presence 检查）

## 3. IDE 插件接入（P2）

`intellij-ide/modules/ide/base`：

```kotlin
class CangJieInlayHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor) = object : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            // 候选过滤（CjProperty/CjCallExpression…）命中后才 analyze(file){ collector.collect(...) }
            // sink.addPresentation(InlineInlayPosition(offset), ...) 产出
        }
    }
}
```

- 声明式 EP 在平台 253（`ideRunVersion=253.29346.50`）可用；intellij-ide 内无先例，P2 首例
- 注册：`cangjie-editor.xml`（语言注册先例：`<lang.foldingBuilder language="CangJie" .../>`）或新建 `cangjie-inlay.xml` 被 `org.cangnova.cangjie.ide.base.xml` include；`language="CangJie"`（`CangJieLanguage.kt:33` 确认）
- IDE 侧薄壳消费共享 collector 的先例：`CangJieFoldingBuilder.kt:35-55`

## 4. 测试（三类测试层次分开，评审 N8；第四项为第三轮 dry-run 修正）

**A. 共享层单测——结构逻辑**（不需要 session）：候选判定（`CjProperty.isLocal`、`CjVariable.typeReference == null`）、range 裁剪、异常隔离、options 开关分流等纯 PSI 逻辑，沿用 folding 测试的内联字符串风格（`CangJieFoldingRangeCollectorTest`，`CjParsingTestCase(dataPath="")`）。

**B. 共享层单测——类型解析逻辑**（第三轮 dry-run 修正：**不能照抄 folding 的纯解析模式**——类型 hint 依赖 `CaSession.expressionType`，`CjParsingTestCase` 环境无法提供 session）：
- 使用 `analysis:analysis-test-framework` 的 `AbstractAnalysisApiBasedTest`（testFixtures，使用先例：`analysis/stubs` 系列测试），`code-insight/hints` 的 build.gradle.kts 加 `testImplementation(testFixtures(project(":analysis:analysis-test-framework")))`
- 或最简路径：类型解析路径的验证暂由 LSP 集成测试（真实 `analyzeSnapshot` 链）承担，共享层只测结构逻辑；P1 再评估引入 analysis-test-framework
- P0 建议后者（最小改动），golden 契约文件仍先行建立

**C. golden 契约**（评审 E4，替代"跨构建一致性断言"——LSP 测试跑上游构建、IDE 测试跑 IntelliJ Platform 测试框架，两者无法直接共享断言）：
- 在 `code-insight/hints/testData/` 建**少量** `.cj` golden 文件（仅此目录），用源内标记（如 `<# : Int64 #>`）固化期望输出
- 共享层 / LSP / IDE 三方各自把输出渲染成同种标记文本，比对**同一份** golden

**D. LSP 协议契约**：`CangjieProtocolContractIntegrationTest` 模式加 inlayHint 用例（testkit `LspClientCapabilitiesBuilder.kt:57` 已支持）；P0 补 `resolveInlayHint` 兜底用例 + 命名空间白名单同步（§2.1）。
- 构建验证：`gradlew-queue.bat :code-insight:hints:test` → `:lsp:test` → intellij-ide 构建（P2）

## 5. IDE 命名对齐（评审 D3 + 第五轮 D8 修正）

`intellij-ide/modules/ide/base/src/main/resources/inlayProviders/` 已预置 preview 资源，`CangJieBundle.properties:357-371` **已预置全套文案 key**。注意两处预置命名不完全一致（bundle 用 `call.chains`，preview 目录用 `method.chains`），P2 定 providerId **以 bundle key 为准**：

| bundle key（已存在，直接复用） | preview 目录（不一致项加注） | 期 |
|---|---|---|
| `inlay.cangjie.references.types.hints` + optionId 级 key（hints.type.property/.local.variable/.function.return 等 7 个） | `cangjie.references.types.hints/` | P0-P1 对应种类 |
| `inlay.cangjie.values.hints.cangjie.values.ranges` | `cangjie.values.hints/` | P3 |
| `inlay.cangjie.lambdas.hints.hints.lambda.receivers.parameters/.hints.lambda.return` | `cangjie.lambdas.hints/` | P3 |
| `inlay.cangjie.call.chains.hints`（**≠ 目录名 method.chains**） | `cangjie.method.chains/`（建议改目录名对齐 bundle） | P3 |

## 6. 分期（v3 修正接线归属，评审 N3）

§1.1 八处接线归属：**P0 只做第 1、4 项**（根 include + hints build.gradle.kts，LSP 可编译即可）；**P2 做其余 6 项**（第 2、3、5、6 项 prepare 打包链 + 第 7、8 项 intellij-ide 侧）。

| 期 | 内容 | 验证 |
|---|---|---|
| P0 | §1.1 第 1、4 项 + 数据模型/`CjTypeHintsCollector`（类型 hint）+ LSP 实装 + `RESOLVE_INLAY_HINT` 枚举与 `resolveInlayHint` 兜底 override + **`CangjieLspRequestCoverageTest` 白名单加 `"inlayHint"`** + capabilities 切 `InlayHintRegistrationOptions(resolveProvider=true)` | `gradlew-queue.bat :code-insight:hints:test` + `:lsp:test` 契约用例 |
| P1 | 参数名 hint（`CjParameterNameHintsCollector`，exclude-list 预留扩展点） | 同上 |
| P2 | §1.1 第 2、3、5、6、7、8 项（prepare 打包链 + intellij-ide substitution/catalog/依赖）+ IDE 声明式 provider + providerId 对齐 §5 | intellij-ide 构建 + IDE hint 测试 |
| P3 | resolve 会话存储实装（含协商层 resolveSupport 检查）、设置下发（descriptor flag / didChangeConfiguration）、lambda/链式/values hint | 增量 |

## 7. 评审修正记录

第一轮（A4/A5/B3/C3/D3/E1/E2/E3/E4/F1/F2/F3）：全部落实，见 v2。

第二轮复审（v3 落实）：
- N1（严重）：`InlayHintOptions` → `InlayHintRegistrationOptions`（lsp4j 1.0.0 无前者）
- N2（严重）：P0 新增 `CangjieLspRequest.RESOLVE_INLAY_HINT` 枚举项并经 requestExecutor
- N3（严重）：分期矛盾——P0 只做 §1.1 第 1、4 项，P2 做其余 6 项
- N4（建议）：resolveCompletionItem 约定行号更正为 facade :371-385 / :432-440
- N5（建议）：§1.1 第 4 项依赖 scope 改为 implementation（folding 实况）
- N6（建议）：补注 frameworkDefaults 已置 true，勿重复添加
- N7（建议）：analyzeSnapshot 伪代码 session 实参写 this
- N8（建议）：§4 测试拆分为 A/B/C 三段，目录约定不再混淆
- N9（建议）：§1.4 注明 P0 仅实现类型收集器，防止过度实现

第三轮 dry-run 复审（v4 落实）：
- D1（严重）：`CangjieLspRequestCoverageTest.lspMethodsUseProtocolNamespace` 命名空间白名单（:65）不含 `inlayHint`，新增枚举会挂测试——P0 同步加白名单（§2.1、§6）
- D2（严重）：folding 式纯解析测试环境无法提供 `CaSession`，类型 hint 收集器不能照抄该模式——§4 拆为"结构逻辑纯解析测试 + 类型解析走 LSP 集成测试（P0 最简）或 analysis-test-framework（P1 评估）"
- D3（确认）：其余推演点全部可直接执行——build.gradle.kts 模板（folding 全文）、`CjProperty.isLocal`（:52）/`CjVariable.typeReference`（:202）、`CaExpressionTypeProvider` 接口内扩展属性（analyze 块内直接调）、`CangjieRequestStatistics` 用 `entries.associateWith` 自动覆盖新枚举无需改动

第四轮逐文件审查（v5 落实）：
- D4（严重→已定位策略）：resolve 兜底遗漏 facade 层链路——修正为 §2.1 三层链路（枚举 + 接口方法 + Abstract 默认体 `= hint`，禁用 `unsupported()` 模式）；两个子类自动继承，testkit 零改动
- D5（确认）：lsp4j 1.0.0 细节经 jar 反编译核实——`InlayHintParams.getRange(): Range`、`InlayHint(Position, Either<String, List<InlayHintLabelPart>>)` 构造器、`setTooltip(String)`、`setPaddingLeft/Right(Boolean)`、`setData(Object)`
- D6（确认）：`AnalysisApiPsiSnapshot.psiFile: CjFile`（`AnalysisApiLspSemanticSupport.kt:84`），collector.collect 直接取 `snapshot.psiFile`；range→offset 用 `analysisOffsetAt(range.start)` + `analysisOffsetAt(range.end)`（`LspTextDocument.kt:73/78`）
- D7（确认）：converter 落位 `lsp/src/.../lsp/hints/` 新包（与 `semantic/`、`completion/` 平级）

第五轮平台依赖审查（v6 落实）：
- D8（重要）：`CangJieBundle.properties:357-371` 已预置全套文案 key，P2 直接复用；bundle（`call.chains`）与 preview 目录（`method.chains`）命名不一致，§5 已修——P2 以 bundle 为准
- D9（确认，P2 编译前提）：声明式 API 位于 `com.jetbrains.intellij.platform:lang:253.29346.379`（jar 反编译验证），`dependencies/intellij-core` 已 `api(lang)`——`:code-insight:hints` 与 IDE provider 均可编译；253 接口与方案伪代码完全吻合：`InlayHintsProvider.createCollector(PsiFile, Editor): InlayHintsCollector`（无泛型新版，PossiblyDumbAware 子接口）、`SharedBypassCollector.collectFromElement(PsiElement, InlayTreeSink)` + default `collectFromElementForActions`
- D10（确认）：`intellijSdkVersion=253.29346.379`（gradle.properties:18）；ide/base 走 IntelliJ Platform Gradle Plugin bundledModules 完整平台依赖，P2 无依赖风险

第六轮端到端与惯例审查（v7 落实）：
- D11（对齐确认）：方案符合仓库既有架构决策——completion 提案确立的"IDE 与 LSP 是同一候选生成实现的两个消费端"原则，已写入文档头部架构对齐声明
- D12（遗漏→已补）：接线清单原遗漏第三宿主 **deveco**——§8 声明 P0/P2 范围不含 deveco，后续需要时单独开任务（deveco 扁平 plugin.xml 手工注册，过期注册是静默失效，必须实测验证）
- D13（健壮性→已补）：inlayHints 实装须接取消检查（facade :307-329 的 `throwIfCancelledRecording` + `ProcessCanceledException` 惯例），已写入 §2 步骤 2
- D14（确认）：越界防护已有——`LspTextDocument` offset/position 转换自带 coerce（:132-170），PSI offset 天然合法，converter 无需额外防护
- D15（遗漏→已补）：新增 §9 P0 验收标准（测试全绿 + 手动冒烟 + 回滚开关）；另按 openspec 惯例补简版 proposal.md

第七轮 P0 实施（v8 落实）：见 §10。

## 8. 影响范围与第三宿主 deveco（第六轮 D12）

- 本变更 P0/P2 宿主为 **:lsp 与 intellij-ide**；**deveco 暂不接线**。仓库既有 completion 提案确认 deveco 是第三宿主（`settings.gradle.kts` substitution + 扁平 plugin.xml 手工注册，注册过期是**静默失效**）——P3 后若需要 deveco 支持，单独开任务，且必须实测验证其 plugin.xml 实际生效（deveco 无构建期失败保护）
- 越界防护确认（D14）：`LspTextDocument.offsetAt/positionAt` 对越界 line/character 自带 coerce（`LspTextDocument.kt:132-170`）；collector 产出的 offset 来自 PSI（天然在 analysisText 范围内），converter 转换无越界风险，无需额外防护

## 9. P0 验收标准（第六轮 D15）

1. `gradlew-queue.bat :code-insight:hints:test` 全绿（结构逻辑单测 + golden 文件就位）
2. `gradlew-queue.bat :lsp:test` 全绿，含：类型缺失用例返回非空 hint（label 形如 `: Int64`）、range 外无 hint、空文件/纯注释文件返回空列表、`resolveInlayHint` 原样返回、`CangjieLspRequestCoverageTest` 白名单通过
3. 手动冒烟：真实 LSP 客户端打开 `.cj` 文件，`let x = 42` 在**变量名之后**显示 `: Int64` 类型 hint（见 §10 D20），显式类型的声明不显示
4. 回滚开关：`supportedFeatures.inlayHints` 置 false 即整体下线（server capabilities 随之不再广告），无需其他改动

## 10. P0 实施记录（第七轮，v8）

P0 已按 §6 落地，验证：`:code-insight:hints:test` 8/8、`:lsp:test` 119 全绿（0 failure / 0 error）。

新增文件：
- `code-insight/hints/`（`build.gradle.kts` + `CangJieInlayHint.kt` + `CangJieInlayHintLabelPart`/`Options` 同文件 + `CangJieInlayHintCollector.kt` + `CjTypeHintsCollector.kt`）
- `code-insight/hints/test/.../CjTypeHintsCollectorCandidatesTest.kt`（8 个纯 PSI 用例）
- `code-insight/hints/testData/typeHints.cj`（golden，源内标记 `<#...#>`）
- `lsp/src/.../lsp/hints/CangjieLspInlayHintConverter.kt`
- `lsp/test/.../CangjieInlayHintIntegrationTest.kt`（5 个端到端用例，含 golden 比对与 resolve 兜底）

实施期修正（与本文前九节的偏差，均已在代码中处理）：
- D16（重要）：**`let x = 42` 是 `CjPatternVariable`（`CjVariable`）而非 `CjProperty`**——`CjProperty` 对应 `prop`/`mut prop`，成员字段是 `CjFieldVariable`。§1.4 的"CjProperty（含局部）"据此改为以 `CjVariableDeclaration` 为候选入口（三种 PSI 的统一抽象），局部判定用 `CjPatternVariable.isLocal`。
- D17（重要）：**显式类型判定改用 `colon != null`，不用 `typeReference`**——`CjProperty.typeReference` 在 stub 缺类型节点时走 `LOG.error`（`CjProperty.kt:95-107`），在测试环境下会直接抛断言；`colon` 是纯结构判定且对三种 PSI 一致。
- D18（重要，测试同步）：`CangjieCapabilityNegotiationIntegrationTest` 原断言 `inlayHintProvider.isLeft`，切到 `InlayHintRegistrationOptions` 后必挂；已改为 `isRight` + `resolveProvider == true`。§2.1 的"P0 同步项"清单应增列此项。
- D19（建议）：`resolveInlayHint` 的兜底默认体放在 **`CangjieAnalysisFacade` 接口**，不在 `AbstractCangjieAnalysisFacade` 重复——与既有 `resolveCompletionItem`/`resolveCodeAction` 的位置一致，两个子类同样自动继承。
- D20（建议）：变量提示位置取**声明名末尾**（Kotlin 风格 `let x: Int64 = 42`），不是字面意义的"行尾"；§9 第 3 条已据此改写。
- D21（建议）：函数返回类型为 `Unit` 时不产出提示（省略返回类型的常态，显示只是噪声）；变量类型不受此限。
- D22（补充）：testkit 需两处 helper 才能测 resolve——`LspIntegrationTestConnection.resolveInlayHint` 与 `LspIntegrationTestSession.resolveInlayHint`。

## 11. P1 实施记录（第八轮，v9）

P1（参数名提示）已落地。验证：`:code-insight:hints:test` 14/14、`:lsp:test` 121/121 全绿。

新增/改动：
- 新增 `code-insight/hints/src/.../CjParameterNameHintsCollector.kt`
- 新增 `code-insight/hints/src/.../CjInlayHintFilters.kt`：把「range 裁剪 / 错误节点 / 宏展开」三条共用过滤从类型收集器抽出，两个收集器共用一份规则
- 新增 `code-insight/hints/test/.../CjParameterNameHintsCollectorCandidatesTest.kt`（6 个纯 PSI 用例）
- 新增 `code-insight/hints/testData/parameterNames.cj`（golden）
- `CangJieInlayHintCollector` 在 `options.showParameterNames` 打开时并入参数名提示
- `CangjieInlayHintIntegrationTest` 增至 7 个用例（+ 参数名 golden、+ 命名实参抑制）

实施要点：
- D23（重要）：形参名来源是 **`CaFunctionCall.valueArgumentMapping`**（`Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>>`），名字取 `signature.symbol.name.asString()`，**出 `analyze` 块前转成 `String`**——`CaVariableSignature` 是 lifetime owner，不能带出。
- D24（重要）：提示插在**实参表达式起点**，标签形如 `value: `（含尾随空格）。`inout x` 这类修饰下锚点取表达式而非 `CjValueArgument` 起点，避免提示把 `inout` 与实参切开。
- D25（建议）：命名实参（`name: value`）在**结构层**就排除，不进实参映射查找；括号外尾随 lambda 因为不在 `valueArgumentList.arguments` 里天然不参与。
- D26（建议）："exclude-list 扩展点"落在 `isSuppressed(expression, name)` 一处，当前规则是「实参文本与形参同名则不提示」（`f(count: count)` 无信息量）；后续接 IDE 设置只改这一个函数。

## 12. P2/P3 实施记录（第九、十轮，v10/v11）

P2 已完成 **打包链 + intellij-ide 接线 + 声明式 provider + EP 注册 + 运行时分发**；
P3 第一批完成 **逐项 optionId 开关**（4 项，见 D29）。

验证：
- 根工程 `:prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-hints-for-ide:jar` 可构建
- `:code-insight:hints:test` **16/16 全绿**
- `:lsp:test --tests "*CangJieInlayHintIntegrationTest*"` **7/7 全绿**
- intellij-ide `:modules:ide:base:compileKotlin` / `compileTestKotlin` **BUILD SUCCESSFUL**
- `:modules:ide:base:test --tests "*CangJieInlayHintsProviderTest"` **7/7 全绿**
- `prepareSandbox` 后插件分发目录含 `hints-1.1.1.jar`，打包后 `cangjie-editor.xml` 含 4 个 `<option>`

新增：
- `prepare/ide-plugin-dependencies/cangjie-frontend-code-insight-hints-for-ide/build.gradle.kts`（只打 hints 自身 jar）
- `prepare/ide-plugin-dependencies-module/cangjie-frontend-code-insight-hints-for-ide-module/build.gradle.kts`
- `intellij-ide/modules/ide/base/src/main/kotlin/.../ide/hints/CangJieSharedInlayHintsProvider.kt` + 两个具体 provider
- `intellij-ide/modules/ide/base/src/test/kotlin/.../ide/hints/CangJieInlayHintsProviderTest.kt`（5 个用例，全绿）
- 根 `settings.gradle.kts` 两处 include、`intellij-ide/settings.gradle.kts` substitution、
  `intellij-ide/gradle/libs.versions.toml`、`modules/ide/base/build.gradle.kts` compileOnly + testImplementation、
  `META-INF/cangjie-editor.xml` 两条 `codeInsight.inlayProvider`、`messages/CangJieBundle[_zh_CN].properties` 各一条文案

实施要点：
- D27（重要，偏离 §3 伪代码）：IDE 侧用 **`SharedBypassCollector` + 一次性标记**，而非逐元素收集。
  共享收集器是整文件粒度（`collectDescendantsOfType`），平台对文件内每个元素都会回调一次，
  不设标记会退化成 O(n²) 重复分析。该标记同时让「首次回调即产出全部提示」成立，不依赖平台回调顺序。
- D28（重要）：**`InlayHintsProvider`（253）没有设置泛型参数**，签名是
  `createCollector(PsiFile, Editor): InlayHintsCollector?`，sink 只在 `collectFromElement` 里给；
  §3 伪代码省略了 `file`/`editor` 之外的上下文，实际实现以 `createCollector` 捕获的 `file` 为准。
  另：provider **不实现 `DumbAware`**——提示依赖索引与 CFIR，dumb 模式下应整体跳过。
- D29（已落地）：逐项 optionId 开关已接通。共享层新增 `CangJieInlayHintTarget`（语义种类），
  `CangJieInlayHint` 携带该字段；IDE 侧 `CangJieSharedInlayHintsProvider.optionIdOf` 把种类映射成
  IntelliJ `optionId`，并用 `sink.whenOptionEnabled(optionId) { ... }` 产出——**不自读设置**，
  否则会绕过设置页的即时刷新。已注册 4 项：local.variable / variable / property / function.return。
  未注册的 3 项及原因：`function.parameter`（仓颉形参必须写类型，永不产出）、
  `main.function.return`（与普通返回无差别）、`pattern.variable`（见 D37）。
- D30（已解决，实现细节）：IDE 侧测试必须跑在**后台线程的读动作**里。`analyze` 在 EDT 上会抛
  `ProhibitedAnalysisException: Analysis is not allowed: Called in the EDT thread`，
  最初在 EDT 上跑导致提示全空（表现为「无异常但结果为空」）；改为
  `executeOnPooledThread + ReadAction.compute` 后 5/5 通过。生产路径本身没问题——
  declarative pass 由 `DaemonCodeAnalyzer` 在后台读动作中创建（`submitInBackground` + `runReadAction`）。
  另：`InlayTreeSink.addPresentation` 的真实签名是
  `(position, payloads: List<InlayPayload>?, tooltip: String?, hintFormat: HintFormat, builder: PresentationTreeBuilder.() -> Unit)`，
  与 §3 伪代码的参数名/可空性不同，覆写顺序以 jar 为准。
- D32（重要，运行时崩溃）：**`compileOnly` 只解决编译，插件分发的运行时 jar 在
  `intellij-ide/build.gradle.kts` 的 `runtimeOnly` 收口**。只加 `modules/ide/base` 的 compileOnly
  会让 provider 在编辑器里实例化失败：
  `NoClassDefFoundError: org/cangnova/cangjie/codeinsight/hints/CangJieInlayHintOptions`
  （EP 解析成功、构造 provider 时才炸）。需三处同步：
  ① 根 `runtimeOnly(libs.cangjieCodeInsightHintsForIde)`（随插件分发）
  ② 根 `testImplementation`（插件级测试）
  ③ `modules/test-support` 的 `testRuntimeOnly`（它聚合 `ide/base` 描述符，compileOnly 类同样要显式声明）。
  验证：`prepareSandbox` 后 `plugins/intellij-cangjie/lib/` 出现 `hints-1.1.1.jar` 与
  `cangjie-frontend-code-insight-hints-for-ide-module-1.1.1.jar`。
- D33（环境）：IDE 复合构建编译上游模块对并发/内存敏感，多 Gradle 会话并行时会报
  `Internal compiler error` 或成片 `Unresolved reference`（`CjElement` / `firstIsInstance`）。
  用 `--max-workers=2` 且不与其他构建并跑可稳定通过；这不是本变更的缺陷。
- D34（重要，EP 名踩坑）：**声明式 provider 必须注册在 `codeInsight.declarativeInlayProvider`**。
  `codeInsight.inlayProvider` 是旧接口 `com.intellij.codeInsight.hints.InlayHintsProvider` 的 EP
  （bean 为 `InlayHintsProviderExtensionBean`，与声明式的同名类不同包）。注册到旧 EP 时
  EP 解析、bean 实例化都"成功"，直到 daemon 创建 inlay hint pass 才抛
  `ClassCastException: CangJieTypeInlayHintsProvider cannot be cast to ...hints.InlayHintsProvider`——
  编译期与单测都拦不住。已加回归断言 `testProvidersRegisteredInDeclarativeEp`：
  从 EP 取出 `providerId` 以 `cangjie.` 开头的 bean，断言 id 集合与 `instance` 类型，
  这条用例是唯一能在测试层提前发现 EP 用错的守卫。
- D35（重要，编译器坑）：`private fun InlayTreeSink.addHintPresentation(hint)` 这种「私有扩展函数 +
  内部再传 receiver lambda（`PresentationTreeBuilder.() -> Unit`）」的写法会让 Kotlin 编译器
  直接 ICE（`Internal compiler error`，无 file/line）。改成普通函数 `addHintPresentation(sink, hint)` 即通过。
  另：`for (hint in hints)` 也比 `hints.forEach { ... }` 更稳（后者在本例同样触发过 ICE）。
- D36（重要，EP 结构）：`<option>` 必须**直接**挂在 `codeInsight.declarativeInlayProvider` 下，
  **没有** `<options>` 包裹层；属性名是 `isEnabledByDefault`（不是 `enabledByDefault`）。
  依据是平台自带 provider 的真实写法（database 插件 `sql.column.names.inlay.hints`），
  写错时 bean 的 `options` 会静默为空——EP 解析不报错，只是开关不出现。
- D37（已回退）：解构模式（`let (a, b) = (1, 2)`）的逐绑定类型提示暂不实现。
  绑定不是引用，`CjBindingPattern` 的 `resolveSymbol()` 在 dangling 会话里返回空，
  而整体初始化器是 typle 类型不能代替。保留 `PATTERN_VARIABLE_TYPE` 之外的枚举已删除；
  待 Analysis API 提供「绑定 → 变量符号」入口后单独开。
- D38（测试坑）：`CangJieLightPlatformCodeInsightFixtureTestCase` 是 JUnit3 基类，
  `assertEquals(a, b, "说明")` 会被解析成 `assertEquals(message, expected, actual)`，
  导致 message 被当成期望值。带说明的断言一律写「两参 `assertEquals` + `assertTrue`」。

- D31（补充）：`CangJieBundle_zh_CN.properties` 里 `inlay.cangjie.references.types.hints.hints.type.pattern.variabl`
  少一个 `e`（key 打错，对应选项文案会退化为 key 本身），已顺手修正。


## 13. 后续批次的前置调研（第十一轮，v12）

在独立工作树 `feature/inlay-hints-next` 上继续。动工前先核实了两个「下一项」的真实前置条件，
结论与本提案早期评估有出入，记录在此以免重复踩：

- D39（已查清，非阻塞）：注解**语义入口其实存在**——`CaAnnotatedSymbol.annotations: CaAnnotationList`
  （`CaAnnotationList : List<CaAnnotation>`），配合 `CaAnnotation.arguments: List<CaNamedAnnotationValue>`
  （形参名）与 `CaAnnotationValue.sourcePsi`（实参表达式），足以把「源码位置实参 → 形参名」建起来，
  **不需要**改 analysis 层。真正卡住的是 PSI 层（见 D41）。
- D40（待确认：区间值提示）：PSI 侧完备（`CjRangeExpressionBase.startExpression/stopExpression/isInclusive`），
  但仓库里的预览 `inlayProviders/cangjie.values.hints/preview.cj` 写的是
  `0/*<# ≤ #>*/../*<# ≤ #>*/10`，在 `..` **前后各插一个全角 `≤`**。这不是 Kotlin 的 value hint 形态，
  语义上也没区分 `..`（左闭右开）与 `..=`（含右端）。落地前需要先定标：是标端点可读性（`0 ≤ .. ≤ 10`），
  还是标开闭性（`[0, 10)`）。**不建议照抄预览**。
- D41（阻塞，实测）：**`@X` 在 PSI 层普遍被解析成 `MACRO_EXPRESSION`，不是 `CjAnnotation`**。
  用三个位置的探针实测（`collectDescendantsOfType<CjAnnotation>()` 计数 / `MACRO_EXPRESSION` 计数）：

  | 源码 | CjAnnotation | MACRO_EXPRESSION |
  |---|---|---|
  | `@Ann(1, 2)` + `func demo()` | 0 | 1 |
  | `@Ann[1, 2]` + `func demo()` | 0 | 1 |
  | `@Ann` + `func demo()` | 0 | 1 |
  | `class C { @Ann prop x: Int64 }` | 0 | 1 |
  | `func f(@Ann a: Int64)` | **1** | 0 |
  | `@C` + `foreign func g()` | **1** | 0 |

  即：声明前缀位置的 `@X` 先被宏语法吃掉，只有参数位置与部分 `foreign` 前才产出真正的注解节点。
  注解实参名提示因此**覆盖不完整**，已实现一版后回退（`CjAnnotationParameterNameHintsCollector`
  与接入点均已删除，不留死代码）。
  要恢复这条线，前置是**消解 `@X` 与宏声明的语法歧义**（解析器层改动，独立提案），
  不是提示层的活。


## 14. P4 实施记录：区间开闭性提示（第十二轮，v13）

D40 的定标问题在本轮解决，并已落地共享层与 LSP 宿主。

- D42（定标，取代 D40 的待定项）：采用**不等式标记**而非区间记法。
  起点恒为闭（`..` 与 `..=` 都包含起点），故左端一律 `≤`；右端按 `..  => <`、`..= => ≤`。
  渲染结果 `0 ≤ .. < 10` / `0 ≤ .. ≤ 10`。
  否决 `[0, 10)` 的理由：切片 `a[0..10]` 自带方括号，补方括号会渲染成 `a[[0..10)]`；
  不等式形式与宿主上下文无关，切片与普通范围可共用同一套文本。
  预览资源 `inlayProviders/cangjie.values.hints/preview.cj` 原写 `≤ .. ≤`，对 `..` 是错误的，
  IDE 侧接线时须同步改成 `≤ .. <`（该文件属 `intellij-ide` 仓库，见 D44）。
- D43（实现）：`CjRangeValueHintsCollector` 复用「候选（纯 PSI）+ 解析（`analyze` 内）」两层结构。
  候选同时覆盖 `CjRangeExpression`（`0..10`、含步长 `0..10:2`）与 `CjSliceExpression`（`a[0..10]`、`a[..10]`）；
  缺端点时只提示存在的一端。`session` 仅用于宏展开过滤，本类提示不依赖类型信息。
  端点判定顺序有硬约束：`hasErrorAncestor` 必须在读 `rangeOperatorNode` 之前，
  因为残缺节点取运算符会直接 `error(...)`。
- D44（已完成）：IDE 侧接线落在 `intellij-ide` 仓库的独立工作树
  `cangjie-wt-hints/intellij-ide`（分支 `feature/inlay-hints-next`）：
  ①`optionIdOf` 增加 `RANGE_VALUE -> "cangjie.values.ranges"`；
  ②新增 provider `cangjie.values.hints`（`CangJieValueInlayHintsProvider`，`group=VALUES_GROUP`）+ EP `<option>`；
  ③预览改成 `0/*<# ≤ #>*/../*<# < #>*/10`，并按约定新增逐项预览 `cangjie.values.hints/cangjie.values.ranges.cj`；
  ④类型/参数名 provider 显式 `showRangeValues = false`，否则同一区间会被三个 provider 重复提示；
  ⑤补 zh_CN 文案（含此前缺失的 `hints.type.property`）。
  该仓库改动引用 `RANGE_VALUE`，`-Pcangjie.upstream.root=<根工作树>` 指向上游即可在本分支上独立编译。

验证：
- `:code-insight:hints:test` → **22/22**（新增 `CjRangeValueHintsCollectorCandidatesTest` 6 例）
- `:lsp:test --tests "*CangjieInlayHintIntegrationTest*"` → **8/8**（新增 `rangeValues.cj` golden）
- golden 只比对 `kind == null`（协议里 `OTHER` 无对应类别）的提示，同文件的类型提示不参与
- `intellij-ide`：`:modules:ide:base:compileKotlin` **BUILD SUCCESSFUL**；
  `CangJieInlayHintsProviderTest` **9/9**（新增值 provider 两例 + 更新 EP 注册断言）

环境约束（复现时先看这三条，否则会误判成代码问题）：
- D45（工作树布局）：IDE 测试基类 `CangJiePluginTestCaseBase.locateRepositoryRoot` 靠**向上**找
  `cfir/cfir-serialization/testResources/cjo-sdk/*/std.cjo` 定位仓库根。因此 `intellij-ide` 的
  worktree 必须位于根工作树**内部**（`cangjie-wt-hints/intellij-ide`，正好命中根 `.gitignore`
  的 `/intellij-ide/`），放在兄弟目录会 `Cannot locate repository root`。
  放进去后 `includeBuild("..")` 自然指向根工作树，无需再传 `-Pcangjie.upstream.root`。
- D46（沙箱）：IDE 构建要读根工作树的 `psi/build/classes/**`，agent 沙箱默认拒绝跨目录 file-read，
  表现为 **Gradle daemon 无故消失**（不是 OOM）。跨工作树构建必须在沙箱外执行。
- D47（磁盘）：C 盘曾被 Gradle daemon 日志占满到 0 字节，同样表现为 daemon 猝死；
  `~/.gradle/daemon/*/*.log` 可安全清理。

## 15. P5 实施记录：lambda 返回类型提示（第十三轮，v14）

P3 剩余的 `cangjie.lambdas.hints` 只落地了 `hints.lambda.return`，接收者那一项被证伪后搁置。

- D48（阻塞，实测）：**`hints.lambda.receivers.parameters` 取不到接收者类型**。
  `CaFunctionType` 只暴露 `parameterTypes` / `returnType`，没有 receiver 槽位；
  `expression.expectedType` 拿到的同样是函数类型，无法还原 `this: String` 里的 `String`
  （仓颉的接收者写在 `CjFunctionTypeReceiver`，但那是**类型注解**侧的 PSI，
  调用点推断出的 `CaFunctionType` 已经把它抹平）。要恢复这条线，前置是 Analysis API
  在函数类型上暴露接收者，属于 analysis 层改动，不在提示层做。
- D49（实现）：`CjLambdaHintsCollector` 沿用「候选（纯 PSI）+ 解析（`analyze` 内）」两层结构。
  候选 = 无显式返回类型 + 有函数体 + 末语句不是 `return`；返回值类型取
  `bodyExpression.lastStatement.expressionType`。`Unit` 与类型错误一律不产出
  （`forEach { println(it) }` 这类噪音占绝大多数）。
  文本沿用类型提示的 `: Type` 形态，锚在末表达式之后（`i + 20: Int64`）。
- D50（测试夹具坑）：`values.forEach { value -> ... }` 在 LSP 集成夹具里产不出提示——
  `forEach` 在该 SDK 夹具下解析不到，`hasErrorAncestor` 把整个 lambda 过滤掉了。
  这不是收集器的缺陷，而是夹具的 stdlib 覆盖不全。golden 改用**不依赖成员解析**的写法：
  `let f: (Int64) -> Int64 = { i: Int64 => i + 1 }`。顺带记录：golden 标记里**不写空格**
  （`<#: Int64#>`），否则逐字符比对必然不合。
- D51（环境，非本变更缺陷）：①`gradlew-queue.bat` 在新建的工作树里首跑 bootstrap 会
  构建出 shadowJar 却起不来（`ClassNotFoundException: GradleQueueMainKt`），直连
  `gradlew.bat` 即可；②agent 的 Bash 沙箱在 C 盘吃紧时会整体不可用（连自身二进制都被拒），
  改用 PowerShell 传参数时 `-D` 开头的参数必须放进**字符串数组**，否则会被吞掉前缀。

验证：
- `:code-insight:hints:test` → **28/28**（新增 `CjLambdaHintsCollectorCandidatesTest` 6 例）
- `:lsp:test --tests "*CangjieInlayHintIntegrationTest*"` → **9/9**（新增 `lambdaReturns.cj` golden）
- `intellij-ide`：`CangJieInlayHintsProviderTest` → **11/11**（新增 lambda 行为与 option 声明两例）
- IDE 侧 EP 新增 `providerId=cangjie.lambdas.hints`（`group=TYPES_GROUP`），
  逐项开关 `hints.lambda.return`；其余三个 provider 显式 `showLambdaReturns = false`

