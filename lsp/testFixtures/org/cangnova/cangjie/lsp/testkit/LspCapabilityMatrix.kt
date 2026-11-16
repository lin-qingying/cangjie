package org.cangnova.cangjie.lsp.testkit

import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.DocumentDiagnosticReport
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentHighlight
import org.eclipse.lsp4j.DocumentHighlightKind
import org.eclipse.lsp4j.DocumentRangeFormattingParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.FoldingRange
import org.eclipse.lsp4j.FoldingRangeKind
import org.eclipse.lsp4j.FoldingRangeRequestParams
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.MarkupKind
import org.eclipse.lsp4j.PrepareRenameParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ReferenceContext
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.SelectionRange
import org.eclipse.lsp4j.SelectionRangeParams
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.SemanticTokensRangeParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.WorkspaceDiagnosticParams
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.WorkspaceSymbol
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.eclipse.lsp4j.jsonrpc.messages.Either3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail

/**
 * 当前宣告能力的可审计矩阵。
 *
 * 一份行就是「一项协议入口」：能力开关、协议方法、能力声明检查、请求发送方式、
 * 正向断言、中性断言、所属 facade 入口、真实语义用例位置、状态。
 *
 * 矩阵是测试的**唯一事实来源**：契约测试逐行驱动它，漂移检查用例再反向比对
 * [CangjieLspFeatureSet] 与 [org.cangnova.cangjie.lsp.server.CangjieLspRequest]，
 * 因此漏写一行、写错能力名都会让测试失败，而不是悄悄少覆盖一项能力。
 */
data class LspCapabilityRow(
    /** 矩阵行标识，用于测试报告里定位失败项。 */
    val id: String,
    /** 对应 LSP 方法名。 */
    val protocolMethod: String,
    /**
     * 该入口绑定的能力字段名（[CangjieLspFeatureSet] 的属性名）。
     *
     * 解析类入口复用其所属能力的字段（`completionItem/resolve` 属于 `completion`）。
     */
    val matrixFeature: String?,
    /** 是否以文档 URI 为入口（决定「文档缺失」阶段是否适用）。 */
    val documentScoped: Boolean,
    /** 该入口在测试 facade 里记录的调用名；为空表示不经过 facade。 */
    val facadeEntry: String?,
    /** 能力声明检查：`InitializeResult.capabilities` 里是否广告了该 provider。 */
    val advertised: (ServerCapabilities) -> Boolean,
    /** 发送请求并返回协议结果。 */
    val invoke: (LspIntegrationTestSession, LspMatrixTarget) -> Any?,
    /** 能力开启时的断言。 */
    val assertPositive: (Any?) -> Unit,
    /** 能力关闭 / 文档缺失 / 数据缺失时的中性结果断言。 */
    val assertNeutral: (Any?) -> Unit,
    /** 该行适用的验证阶段。 */
    val stages: Set<LspMatrixStage>,
    /**
     * 广告位期望。
     *
     * 不用「是否跳过校验」的布尔开关：跳过校验收不到任何信息，
     * 而 `FORBIDDEN` 仍然是一条可失败的断言（服务端哪天开始乱广告会立刻暴露）。
     */
    val advertisement: LspAdvertisement = LspAdvertisement.REQUIRED,
    /** 真实语义集成断言所在位置（`类#方法`），`null` 表示只有契约级覆盖。 */
    val semanticCoveredBy: String? = null,
    /** 覆盖状态。 */
    val status: LspCapabilityStatus = LspCapabilityStatus.CONTRACT_ONLY,
    /** 备注：已知缺陷、阶段裁剪原因等需要被审计的信息。 */
    val note: String = "",
) {
    /** 当前行绑定的能力在该能力集合下是否开启。 */
    fun isEnabled(features: CangjieLspFeatureSet): Boolean =
        matrixFeature?.let { name -> LspCapabilityMatrix.featureGetters.getValue(name)(features) } ?: true
}

/** 矩阵行适用的验证阶段。 */
enum class LspMatrixStage {
    /** 双侧能力开启：声明 + 路由 + 正向结果。 */
    ENABLED,

    /** facade 关闭该能力：不声明、返回中性值、不进入 facade。 */
    DISABLED,

    /** 文档未打开：返回中性值、不进入 facade。 */
    MISSING_DOCUMENT,

    /** facade 抛错：请求以协议错误结束，不挂起、不返回伪成功。 */
    FACADE_FAILURE,
}

/**
 * provider 广告位期望。
 */
enum class LspAdvertisement {
    /** 能力开启时必须广告。 */
    REQUIRED,

    /** 必须不广告（服务端有意不声明，例如 pull 诊断 provider）。 */
    FORBIDDEN,
}

/** 覆盖状态。 */
enum class LspCapabilityStatus {
    /** 契约测试 + 真实语义断言都有。 */
    CONTRACT_AND_SEMANTIC,

    /** 只有协议契约测试（真实语义由其它模块或占位实现承担）。 */
    CONTRACT_ONLY,

    /** 服务端已宣告但实现未接入：按已宣告契约断言，缺陷留在矩阵里。 */
    DEFECT_RECORDED,
}

/**
 * 矩阵请求上下文的定位信息。
 */
data class LspMatrixTarget(
    /** 已打开文档 URI。 */
    val uri: String,
    /** 未打开文档 URI。 */
    val missingUri: String,
    /** 请求使用的文档内位置。 */
    val position: Position = Position(0, 0),
)

/**
 * 当前服务端宣告能力的矩阵定义。
 */
object LspCapabilityMatrix {
    /**
     * [CangjieLspFeatureSet] 属性名到读取函数的映射。
     *
     * 漂移检查用例拿它的 key 集合与数据类字段比对，因此新增能力却忘了补矩阵会直接失败。
     */
    val featureGetters: Map<String, (CangjieLspFeatureSet) -> Boolean> = linkedMapOf(
        "completion" to { it.completion },
        "hover" to { it.hover },
        "signatureHelp" to { it.signatureHelp },
        "declaration" to { it.declaration },
        "definition" to { it.definition },
        "typeDefinition" to { it.typeDefinition },
        "implementation" to { it.implementation },
        "references" to { it.references },
        "documentHighlight" to { it.documentHighlight },
        "documentSymbol" to { it.documentSymbol },
        "workspaceSymbol" to { it.workspaceSymbol },
        "codeAction" to { it.codeAction },
        "formatting" to { it.formatting },
        "rename" to { it.rename },
        "foldingRange" to { it.foldingRange },
        "selectionRange" to { it.selectionRange },
        "semanticTokens" to { it.semanticTokens },
        "inlayHints" to { it.inlayHints },
        "diagnostics" to { it.diagnostics },
    )

    /**
     * 生命周期通知，不属于「请求入口」，由生命周期用例覆盖。
     */
    val lifecycleMethods: Set<String> = setOf(
        "initialize",
        "textDocument/didOpen",
        "textDocument/didChange",
        "textDocument/didSave",
        "textDocument/didClose",
        "textDocument/publishDiagnostics",
    )

    /** 契约 facade 使用的固定文档 URI。 */
    const val DOCUMENT_URI: String = "file:///workspace/contract.cj"

    /** 契约 facade 使用的缺失文档 URI。 */
    const val MISSING_URI: String = "file:///workspace/missing.cj"

    /** 当前矩阵的测试目标。 */
    val target: LspMatrixTarget = LspMatrixTarget(DOCUMENT_URI, MISSING_URI)

    /**
     * 全部矩阵行。
     */
    val rows: List<LspCapabilityRow> = buildList {
        add(
            row(
                id = "completion",
                protocolMethod = "textDocument/completion",
                feature = "completion",
                facadeEntry = "completion",
                advertised = { it.completionProvider != null },
                invoke = { session, target -> session.completion(session.completionParams(target.uri, 0, 0)) },
                positive = { result ->
                    val items = eitherLeft(result)
                    assertEquals("contractCompletion", (items.single() as org.eclipse.lsp4j.CompletionItem).label)
                },
                neutral = ::assertEmptyProtocolResult,
                semantic = "CangjieSemanticFeatureIntegrationTest#editor assist requests resolve against real analysis workspace",
            ),
        )
        add(
            row(
                id = "completionItem/resolve",
                protocolMethod = "completionItem/resolve",
                feature = "completion",
                documentScoped = false,
                facadeEntry = "resolveCompletionItem",
                advertised = { it.completionProvider != null },
                invoke = { session, _ -> session.resolveCompletionItem(org.eclipse.lsp4j.CompletionItem("contractCompletion")) },
                positive = { result ->
                    val item = result as org.eclipse.lsp4j.CompletionItem
                    assertEquals("contractCompletion", item.label)
                    assertEquals(
                        "contractCompletionDocumentation",
                        (item.documentation.right as MarkupContent).value,
                    )
                },
                neutral = { result ->
                    val item = result as org.eclipse.lsp4j.CompletionItem
                    assertEquals("contractCompletion", item.label)
                    assertNull(item.documentation, "能力关闭时 resolve 只能原样返回候选")
                },
                stages = setOf(LspMatrixStage.ENABLED, LspMatrixStage.DISABLED, LspMatrixStage.FACADE_FAILURE),
                semantic = "LspCompletionResolveTest#resolverRecoversTheSameDeclarationByCandidateIdentity",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "hover",
                protocolMethod = "textDocument/hover",
                feature = "hover",
                facadeEntry = "hover",
                advertised = { it.hoverProvider.advertises() },
                invoke = { session, target -> session.hover(session.hoverParams(target.uri, 0, 0)) },
                positive = { result ->
                    val hover = result as Hover
                    val markup = hover.contents.right as MarkupContent
                    assertEquals(MarkupKind.MARKDOWN, markup.kind)
                    assertEquals("```cangjie\ncontractHover\n```", markup.value)
                },
                neutral = { result ->
                    val hover = result as Hover
                    assertTrue(hover.contents.isLeft && hover.contents.left.isEmpty())
                },
                semantic = "CangjieSemanticFeatureIntegrationTest#editor assist requests resolve against real analysis workspace",
            ),
        )
        add(
            row(
                id = "signatureHelp",
                protocolMethod = "textDocument/signatureHelp",
                feature = "signatureHelp",
                facadeEntry = "signatureHelp",
                advertised = { it.signatureHelpProvider != null },
                invoke = { session, target -> session.signatureHelp(session.signatureHelpParams(target.uri, 0, 0)) },
                positive = { result ->
                    assertEquals(
                        "contractSignature(first: Int64, second: Int64)",
                        (result as SignatureHelp).signatures.single().label,
                    )
                },
                neutral = { result -> assertTrue((result as SignatureHelp).signatures.isEmpty()) },
                semantic = "CangjieSemanticFeatureIntegrationTest#editor assist requests resolve against real analysis workspace",
            ),
        )
        add(locationRow("declaration", "textDocument/declaration", "declaration", "declaration"))
        add(locationRow("definition", "textDocument/definition", "definition", "definition"))
        add(locationRow("typeDefinition", "textDocument/typeDefinition", "typeDefinition", "typeDefinition"))
        add(locationRow("implementation", "textDocument/implementation", "implementation", "implementation"))
        add(
            row(
                id = "references",
                protocolMethod = "textDocument/references",
                feature = "references",
                facadeEntry = "references",
                advertised = { it.referencesProvider.advertises() },
                invoke = { session, target ->
                    session.references(ReferenceParams(TextDocumentIdentifier(target.uri), Position(0, 0), ReferenceContext(true)))
                },
                positive = { result ->
                    val locations = result as List<*>
                    assertEquals(DOCUMENT_URI, (locations.single() as Location).uri)
                },
                neutral = ::assertEmptyProtocolResult,
                semantic = "CangjieSemanticFeatureIntegrationTest#navigation and symbol requests resolve across workspace files",
            ),
        )
        add(
            row(
                id = "documentHighlight",
                protocolMethod = "textDocument/documentHighlight",
                feature = "documentHighlight",
                facadeEntry = "documentHighlight",
                advertised = { it.documentHighlightProvider.advertises() },
                invoke = { session, target ->
                    session.documentHighlight(session.documentHighlightParams(target.uri, 0, 0))
                },
                positive = { result ->
                    assertEquals(
                        DocumentHighlightKind.Text,
                        (result as List<*>).single().let { it as DocumentHighlight }.kind,
                    )
                },
                neutral = ::assertEmptyProtocolResult,
                semantic = "CangjieSemanticFeatureIntegrationTest#navigation and symbol requests resolve across workspace files",
            ),
        )
        add(
            row(
                id = "documentSymbol",
                protocolMethod = "textDocument/documentSymbol",
                feature = "documentSymbol",
                facadeEntry = "documentSymbol",
                advertised = { it.documentSymbolProvider.advertises() },
                invoke = { session, target -> session.documentSymbol(session.documentSymbolParams(target.uri)) },
                positive = { result ->
                    val symbols = result as List<*>
                    val symbol = symbols.single() as Either<*, *>
                    assertEquals("contractDocumentSymbol", (symbol.right as DocumentSymbol).name)
                },
                neutral = ::assertEmptyProtocolResult,
                semantic = "CangjieSemanticFeatureIntegrationTest#navigation and symbol requests resolve across workspace files",
            ),
        )
        add(
            row(
                id = "workspaceSymbol",
                protocolMethod = "workspace/symbol",
                feature = "workspaceSymbol",
                documentScoped = false,
                facadeEntry = "workspaceSymbol",
                advertised = { it.workspaceSymbolProvider.advertises() },
                invoke = { session, _ -> session.workspaceSymbol(WorkspaceSymbolParams("contract")) },
                positive = { result ->
                    val symbols = eitherRight(result)
                    assertEquals("contractWorkspaceSymbol", (symbols.single() as WorkspaceSymbol).name)
                },
                neutral = ::assertEmptyProtocolResult,
                stages = setOf(LspMatrixStage.ENABLED, LspMatrixStage.DISABLED, LspMatrixStage.FACADE_FAILURE),
                semantic = "CangjieSemanticFeatureIntegrationTest#navigation and symbol requests resolve across workspace files",
            ),
        )
        add(
            row(
                id = "codeAction",
                protocolMethod = "textDocument/codeAction",
                feature = "codeAction",
                facadeEntry = "codeAction",
                advertised = { it.codeActionProvider != null },
                invoke = { session, target ->
                    session.codeAction(
                        CodeActionParams(
                            TextDocumentIdentifier(target.uri),
                            Range(Position(0, 0), Position(0, 4)),
                            CodeActionContext(emptyList()),
                        ),
                    )
                },
                positive = { result ->
                    val actions = result as List<*>
                    val action = actions.single() as Either<*, *>
                    assertEquals("contractCodeAction", (action.right as CodeAction).title)
                },
                neutral = ::assertEmptyProtocolResult,
                semantic = "CangjieCodeActionIntegrationTest#codeAction lists quickfix and resolve produces implement-member edits",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "codeAction/resolve",
                protocolMethod = "codeAction/resolve",
                feature = "codeAction",
                documentScoped = false,
                facadeEntry = "resolveCodeAction",
                advertised = { it.codeActionProvider != null },
                invoke = { session, target ->
                    session.resolveCodeAction(
                        CodeAction().apply {
                            title = "contractCodeAction"
                            data = mapOf(
                                "documentUri" to target.uri,
                                "diagnosticFactoryName" to "contract",
                                "startLine" to 0,
                                "startCharacter" to 0,
                                "endLine" to 0,
                                "endCharacter" to 4,
                            )
                        },
                    )
                },
                positive = { result ->
                    val action = result as CodeAction
                    assertEquals("contractCodeAction", action.title)
                    assertEquals(
                        "contractResolved",
                        action.edit.changes.getValue(DOCUMENT_URI).single().newText,
                    )
                },
                neutral = { result ->
                    val action = result as CodeAction
                    assertEquals("contractCodeAction", action.title)
                    assertNull(action.edit, "能力关闭时 resolve 只能原样返回动作")
                },
                stages = setOf(LspMatrixStage.ENABLED, LspMatrixStage.DISABLED, LspMatrixStage.FACADE_FAILURE),
                semantic = "CangjieCodeActionIntegrationTest#codeAction lists quickfix and resolve produces implement-member edits",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "formatting",
                protocolMethod = "textDocument/formatting",
                feature = "formatting",
                facadeEntry = "formatting",
                advertised = { it.documentFormattingProvider != null },
                invoke = { session, target ->
                    session.formatting(
                        DocumentFormattingParams(TextDocumentIdentifier(target.uri), FormattingOptions(4, true)),
                    )
                },
                positive = { result -> assertEquals("contractFormat", (result as List<*>).single().let { it as org.eclipse.lsp4j.TextEdit }.newText) },
                neutral = ::assertEmptyProtocolResult,
                semantic = "CangjieFormattingIntegrationTest#document formatting returns shared formatter edit and respects indent width",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "rangeFormatting",
                protocolMethod = "textDocument/rangeFormatting",
                feature = "formatting",
                facadeEntry = null,
                advertised = { it.documentRangeFormattingProvider.advertises() },
                invoke = { session, target ->
                    session.rangeFormatting(
                        DocumentRangeFormattingParams(
                            TextDocumentIdentifier(target.uri),
                            FormattingOptions(4, true),
                            Range(Position(0, 0), Position(0, 4)),
                        ),
                    )
                },
                positive = { result ->
                    val edits = result as List<*>
                    assertTrue(
                        edits.isNotEmpty(),
                        "已宣告 documentRangeFormattingProvider，就必须返回覆盖请求范围的编辑；当前返回空列表，" +
                            "等于宣告了一项做不到的能力",
                    )
                    val first = edits.first() as org.eclipse.lsp4j.TextEdit
                    assertTrue(
                        first.range.start.line >= 0 && first.range.end.line <= 0,
                        "范围格式化的编辑必须落在请求范围内：${first.range}",
                    )
                },
                neutral = ::assertEmptyProtocolResult,
                stages = setOf(LspMatrixStage.ENABLED),
                status = LspCapabilityStatus.DEFECT_RECORDED,
                note = "**已知缺陷（用例故意保持失败）**：服务端宣告 documentRangeFormattingProvider，但服务层直接返回空编辑且不进 facade。按计划要求保留红灯，不改生产代码。",
            ),
        )
        add(
            row(
                id = "rename",
                protocolMethod = "textDocument/rename",
                feature = "rename",
                facadeEntry = "rename",
                advertised = { it.renameProvider.advertises() },
                invoke = { session, target ->
                    session.rename(RenameParams(TextDocumentIdentifier(target.uri), Position(0, 0), "renamedValue"))
                },
                positive = { result ->
                    val edit = result as WorkspaceEdit
                    assertEquals("renamedValue", edit.changes.getValue(DOCUMENT_URI).single().newText)
                },
                neutral = { result -> assertTrue((result as WorkspaceEdit).changes.isNullOrEmpty()) },
                semantic = "CangjieRenameIntegrationTest#rename returns declaration and reference workspace edits",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "prepareRename",
                protocolMethod = "textDocument/prepareRename",
                feature = "rename",
                facadeEntry = "prepareRename",
                advertised = { it.renameProvider.advertises() },
                invoke = { session, target ->
                    session.prepareRename(PrepareRenameParams(TextDocumentIdentifier(target.uri), Position(0, 0)))
                },
                positive = { result ->
                    val prepared = result as Either3<*, *, *>
                    assertTrue(prepared.isFirst, "可重命名位置必须回落到 Range 分支")
                    assertEquals(
                        Range(Position(0, 0), Position(0, 4)),
                        prepared.first,
                        "prepareRename 必须给出可重命名的精确范围",
                    )
                },
                neutral = { result ->
                    val prepared = result as Either3<*, *, *>
                    assertTrue(prepared.isFirst, "不可重命名位置必须回落到 Range 分支")
                },
                semantic = "CangjieRenameIntegrationTest#rename returns declaration and reference workspace edits",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "foldingRange",
                protocolMethod = "textDocument/foldingRange",
                feature = "foldingRange",
                facadeEntry = "foldingRange",
                advertised = { it.foldingRangeProvider.advertises() },
                invoke = { session, target -> session.foldingRange(FoldingRangeRequestParams(TextDocumentIdentifier(target.uri))) },
                positive = { result ->
                    assertEquals(FoldingRangeKind.Region, (result as List<*>).single().let { it as FoldingRange }.kind)
                },
                neutral = ::assertEmptyProtocolResult,
                semantic = "CangjieFoldingIntegrationTest#folding range returns real imports comment and multiline call ranges",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "selectionRange",
                protocolMethod = "textDocument/selectionRange",
                feature = "selectionRange",
                facadeEntry = "selectionRange",
                advertised = { it.selectionRangeProvider.advertises() },
                invoke = { session, target ->
                    session.selectionRange(SelectionRangeParams(TextDocumentIdentifier(target.uri), listOf(Position(0, 0))))
                },
                positive = { result ->
                    val ranges = result as List<*>
                    assertNotNull((ranges.single() as SelectionRange).parent)
                },
                neutral = ::assertEmptyProtocolResult,
                semantic = "CangjieSemanticFeatureIntegrationTest#editor assist requests resolve against real analysis workspace",
            ),
        )
        add(
            row(
                id = "semanticTokens/full",
                protocolMethod = "textDocument/semanticTokens/full",
                feature = "semanticTokens",
                facadeEntry = "semanticTokensFull",
                advertised = { it.semanticTokensProvider != null },
                invoke = { session, target -> session.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(target.uri))) },
                positive = { result -> assertEquals(listOf(0, 0, 4, 1, 0), (result as SemanticTokens).data) },
                neutral = { result -> assertTrue((result as SemanticTokens).data.isEmpty()) },
                semantic = "CangJieSemanticTokenEncodingTest#fullDocumentTokensSatisfyDeltaEncodingInvariants",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "semanticTokens/range",
                protocolMethod = "textDocument/semanticTokens/range",
                feature = "semanticTokens",
                facadeEntry = "semanticTokensRange",
                advertised = { it.semanticTokensProvider != null },
                invoke = { session, target ->
                    session.semanticTokensRange(
                        SemanticTokensRangeParams(TextDocumentIdentifier(target.uri), Range(Position(0, 0), Position(0, 4))),
                    )
                },
                positive = { result -> assertEquals(listOf(0, 0, 4, 1, 0), (result as SemanticTokens).data) },
                neutral = { result -> assertTrue((result as SemanticTokens).data.isEmpty()) },
                semantic = "CangJieSemanticTokenEncodingTest#rangeTokensAreSubsetOfFullDocumentTokens",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "inlayHint",
                protocolMethod = "textDocument/inlayHint",
                feature = "inlayHints",
                facadeEntry = "inlayHint",
                advertised = { it.inlayHintProvider != null },
                invoke = { session, target ->
                    session.inlayHint(InlayHintParams(TextDocumentIdentifier(target.uri), Range(Position(0, 0), Position(0, 4))))
                },
                positive = { result ->
                    val hint = (result as List<*>).single() as InlayHint
                    assertEquals("contractHint", hint.label.left)
                    assertEquals(Position(0, 0), hint.position)
                },
                neutral = ::assertEmptyProtocolResult,
                semantic = "CangjieInlayHintIntegrationTest#type hints match shared golden contract",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "inlayHint/resolve",
                protocolMethod = "inlayHint/resolve",
                feature = "inlayHints",
                documentScoped = false,
                facadeEntry = "resolveInlayHint",
                advertised = { it.inlayHintProvider != null },
                invoke = { session, _ ->
                    session.resolveInlayHint(
                        InlayHint().apply {
                            position = Position(0, 0)
                            label = Either.forLeft("contractHint")
                        },
                    )
                },
                positive = { result ->
                    val hint = result as InlayHint
                    assertEquals("contractHintTooltip", hint.tooltip.left)
                },
                neutral = { result ->
                    val hint = result as InlayHint
                    assertNull(hint.tooltip, "能力关闭时 resolve 只能原样返回提示")
                },
                stages = setOf(LspMatrixStage.ENABLED, LspMatrixStage.DISABLED, LspMatrixStage.FACADE_FAILURE),
                semantic = "CangjieInlayHintIntegrationTest#inlay hint resolve returns hint unchanged",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
            ),
        )
        add(
            row(
                id = "diagnostic/pull",
                protocolMethod = "textDocument/diagnostic",
                feature = "diagnostics",
                facadeEntry = "collectDiagnostics",
                advertised = { it.diagnosticProvider != null },
                invoke = { session, target -> session.documentDiagnostic(session.documentDiagnosticParams(target.uri)) },
                positive = { result -> assertTrue(result.toString().contains("contractDiagnostic")) },
                neutral = { result ->
                    val report = result as DocumentDiagnosticReport
                    assertTrue(report.isRelatedFullDocumentDiagnosticReport)
                    assertTrue(report.relatedFullDocumentDiagnosticReport.items.isEmpty())
                },
                // provider 默认为「不广告」（descriptor.pullDiagnosticsEnabled=false），
                // 因此广告位既不能正向断言也无法反向断言，只按请求路径验证。
                stages = setOf(LspMatrixStage.ENABLED, LspMatrixStage.MISSING_DOCUMENT),
                advertisement = LspAdvertisement.FORBIDDEN,
                semantic = "CangjieSemanticDiagnosticsIntegrationTest#overlay dependency changes refresh push and pull diagnostics for dependent document",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
                note = "pull diagnostics provider 默认不广告；请求路径仍按 diagnostics 能力开关工作。",
            ),
        )
        add(
            row(
                id = "diagnostic/push",
                protocolMethod = "textDocument/publishDiagnostics",
                feature = "diagnostics",
                facadeEntry = "collectDiagnostics",
                advertised = { it.textDocumentSync != null },
                invoke = { session, target -> session.awaitPublishedDiagnostics(target.uri) },
                positive = { result ->
                    val published = result as PublishDiagnosticsParams
                    assertEquals(DOCUMENT_URI, published.uri)
                    assertTrue(published.diagnostics.any { it.message.left == "contractDiagnostic" })
                },
                neutral = { _: Any? -> },
                // 关闭能力时不会再有新通知，等待哨兵会超时；该负向路径由诊断集成用例覆盖。
                stages = setOf(LspMatrixStage.ENABLED),
                semantic = "CangjieLanguageServerTest#publishes diagnostics through real lsp connection",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
                note = "push 诊断没有独立 capability 字段（隐式随 textDocumentSync 通道提供）；负向路径由诊断集成用例覆盖。",
            ),
        )
        add(
            row(
                id = "workspace/diagnostic",
                protocolMethod = "workspace/diagnostic",
                feature = "diagnostics",
                documentScoped = false,
                facadeEntry = "collectWorkspaceDiagnostics",
                advertised = { it.diagnosticProvider != null },
                invoke = { session, _ -> session.workspaceDiagnostic(WorkspaceDiagnosticParams()) },
                positive = { result -> assertTrue(result.toString().contains("contractDiagnostic")) },
                neutral = { result -> assertTrue((result as org.eclipse.lsp4j.WorkspaceDiagnosticReport).items.isEmpty()) },
                stages = setOf(LspMatrixStage.ENABLED, LspMatrixStage.DISABLED),
                // 与 pull 一致：diagnosticProvider 默认不广告（descriptor 显式关闭）。
                advertisement = LspAdvertisement.FORBIDDEN,
                semantic = "CangjieSemanticDiagnosticsIntegrationTest#workspace diagnostics include unopened disk files from newly added workspace folders",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
                note = "workspace pull 诊断与 textDocument/diagnostic 共用 diagnosticProvider，默认不广告。",
            ),
        )
        add(
            row(
                id = "workspace/executeCommand",
                protocolMethod = "workspace/executeCommand",
                feature = null,
                documentScoped = false,
                facadeEntry = null,
                advertised = { it.executeCommandProvider != null },
                invoke = { session, _ -> session.executeCommand(ExecuteCommandParams("cangjie.reindex", emptyList())) },
                positive = { result -> assertNotNull(result, "未识别的命令必须返回非空占位结果") },
                neutral = { _: Any? -> },
                stages = setOf(LspMatrixStage.ENABLED),
                semantic = "LspCompletionCommandHandlerTest#applyCompletionSendsApplyEditWithComputedEdits",
                status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
                note = "命令路由不随具体能力开关变化；补全命令的失败路径由补全命令用例覆盖。",
            ),
        )
    }

    /** 按 id 查找矩阵行；不存在时报告矩阵定义错误。 */
    fun require(id: String): LspCapabilityRow =
        rows.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: error("能力矩阵里没有 `$id` 这一行")

    /**
     * 构造文档级跳转类矩阵行（declaration/definition/typeDefinition/implementation 形态一致）。
     */
    private fun locationRow(
        id: String,
        protocolMethod: String,
        feature: String,
        facadeEntry: String,
    ): LspCapabilityRow = row(
        id = id,
        protocolMethod = protocolMethod,
        feature = feature,
        facadeEntry = facadeEntry,
        advertised = {
            when (id) {
                "declaration" -> it.declarationProvider.advertises()
                "definition" -> it.definitionProvider.advertises()
                "typeDefinition" -> it.typeDefinitionProvider.advertises()
                else -> it.implementationProvider.advertises()
            }
        },
        invoke = { session, target ->
            when (id) {
                "declaration" -> session.declaration(session.declarationParams(target.uri, 0, 0))
                "definition" -> session.definition(session.definitionParams(target.uri, 0, 0))
                "typeDefinition" -> session.typeDefinition(session.typeDefinitionParams(target.uri, 0, 0))
                else -> session.implementation(session.implementationParams(target.uri, 0, 0))
            }
        },
        positive = { result ->
            val locations = eitherLeft(result)
            assertEquals(DOCUMENT_URI, (locations.single() as Location).uri)
        },
        neutral = ::assertEmptyProtocolResult,
        semantic = "CangjieSemanticFeatureIntegrationTest#navigation and symbol requests resolve across workspace files",
        status = LspCapabilityStatus.CONTRACT_AND_SEMANTIC,
    )

    /**
     * 统一构造矩阵行的默认值装配。
     */
    private fun row(
        id: String,
        protocolMethod: String,
        feature: String?,
        documentScoped: Boolean = true,
        facadeEntry: String?,
        advertised: (ServerCapabilities) -> Boolean,
        invoke: (LspIntegrationTestSession, LspMatrixTarget) -> Any?,
        positive: (Any?) -> Unit,
        neutral: (Any?) -> Unit,
        stages: Set<LspMatrixStage> = setOf(
            LspMatrixStage.ENABLED,
            LspMatrixStage.DISABLED,
            LspMatrixStage.MISSING_DOCUMENT,
            LspMatrixStage.FACADE_FAILURE,
        ),
        advertisement: LspAdvertisement = LspAdvertisement.REQUIRED,
        semantic: String? = null,
        status: LspCapabilityStatus = LspCapabilityStatus.CONTRACT_ONLY,
        note: String = "",
    ): LspCapabilityRow = LspCapabilityRow(
        id = id,
        protocolMethod = protocolMethod,
        matrixFeature = feature,
        documentScoped = documentScoped,
        facadeEntry = facadeEntry,
        advertised = advertised,
        invoke = invoke,
        assertPositive = positive,
        assertNeutral = neutral,
        stages = stages,
        advertisement = advertisement,
        semanticCoveredBy = semantic,
        status = status,
        note = note,
    )
}

/**
 * 判断 `Either<Boolean, *>` 形态的 provider 字段是否广告了能力。
 */
private fun Either<Boolean, *>?.advertises(): Boolean = this != null && (left == true || isRight)

/**
 * 取出 `Either` 左分支的列表；右分支出现时报告契约不符。
 */
internal fun eitherLeft(result: Any?): List<*> {
    val either = result as? Either<*, *> ?: fail("期望 Either 结果，实际 ${result?.javaClass?.name}")
    assertTrue(either.isLeft, "期望左分支（列表形态），实际是右分支：${either.right}")
    return either.left as? List<*> ?: fail("左分支不是列表：${either.left}")
}

/**
 * 取出 `Either` 右分支的列表；左分支出现时报告契约不符。
 */
internal fun eitherRight(result: Any?): List<*> {
    val either = result as? Either<*, *> ?: fail("期望 Either 结果，实际 ${result?.javaClass?.name}")
    assertTrue(either.isRight, "期望右分支（列表形态），实际是左分支：${either.left}")
    return either.right as? List<*> ?: fail("右分支不是列表：${either.right}")
}

/**
 * 中性结果断言：覆盖当前矩阵里所有会被「关闭能力 / 文档缺失」命中的返回形态。
 */
internal fun assertEmptyProtocolResult(result: Any?) {
    when (result) {
        // null 不是「空结果」：服务端必须先给出空列表/空对象，客户端才能区分
        // 「没有结果」和「响应字段缺失」。放行 null 会让契约测试对空实现也放绿。
        null -> fail("中性结果不得为 null：应返回空列表/空对象")
        is Either<*, *> -> {
            /*
             * 线上只有一个 JSON 数组，`Either` 的左/右只区别元素类型（Location vs LocationLink、
             * SymbolInformation vs WorkspaceSymbol）。因此「没有结果」应看**列表是否为空**，
             * 而不是看落在哪一侧——否则空列表的侧别歧义会变成假失败。
             */
            val empty = if (result.isLeft) {
                (result.left as? Collection<*>)?.isEmpty() ?: true
            } else {
                (result.right as? Collection<*>)?.isEmpty() ?: false
            }
            assertTrue(empty, "中性结果必须是空列表：$result")
        }

        is Collection<*> -> assertTrue(result.isEmpty(), "中性结果必须为空：$result")
        is SemanticTokens -> assertTrue(result.data.isEmpty())
        is DocumentDiagnosticReport -> assertTrue(result.relatedFullDocumentDiagnosticReport.items.isEmpty())
        is org.eclipse.lsp4j.WorkspaceDiagnosticReport -> assertTrue(result.items.isEmpty(), "工作区诊断中性结果必须为空：$result")
        is WorkspaceEdit -> assertTrue(result.changes.isNullOrEmpty())
        is Hover -> assertTrue(result.contents.isLeft && result.contents.left.isEmpty())
        is SignatureHelp -> assertTrue(result.signatures.isEmpty())
        is Either3<*, *, *> -> assertTrue(result.isFirst, "prepareRename 中性结果必须是 Range 分支")
        else -> fail("没有为中性的 ${result.javaClass.name} 定义断言；新增能力时请补齐矩阵")
    }
}

/**
 * 断言矩阵行的失败注入消息确实回到了客户端。
 */
internal fun assertInjectedFailure(failure: Throwable, entry: String) {
    val chain = generateSequence(failure) { it.cause }.toList()
    val protocolError = chain.filterIsInstance<ResponseErrorException>().firstOrNull()
    assertNotNull(
        protocolError,
        "请求 $entry 应以 JSON-RPC 错误结束，实际异常链=${chain.map { it.javaClass.simpleName }}",
    )
    assertEquals(
        ResponseErrorCode.InternalError.value,
        protocolError!!.responseError.code,
        "入口抛出的未预期异常必须变成 InternalError：$entry（实际消息=${protocolError.message}）",
    )
    assertFalse(
        chain.any { it is AssertionError },
        "失败断言混进了协议错误：$failure",
    )
}


/**
 * 已知缺陷用例的 JUnit 标签。
 *
 * 这些用例**故意保持失败**：它们断言的是已宣告契约，而实现尚未满足。
 * 需要绿构建的 CI 可以用 `-PexcludeKnownDefects` 排除该标签，但本地默认必须能看到红灯，
 * 否则缺陷会重新变成「测试全绿」。
 */
const val KNOWN_DEFECT_TAG: String = "known-defect"
