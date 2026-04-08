package org.cangnova.cangjie.lsp.testkit

import org.cangnova.cangjie.lsp.analysis.AbstractCangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisLifecycleContext
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisRequestContext
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.Either3
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 协议契约测试专用的假 AnalysisFacade。
 *
 * 它不模拟真实语义，只做两件事：
 * 1. 按 `supportedFeatures` 暴露能力，驱动 initialize 协商矩阵；
 * 2. 为每个入口返回稳定、可断言的结果，并记录调用轨迹。
 *
 * 这样协议测试可以验证：
 * - feature 开关是否真的影响 capability 暴露；
 * - 请求是否穿过 JSON-RPC 正确抵达 facade；
 * - 服务端在“缺文档 / 关闭 feature / 中性返回”场景下是否稳定。
 */
class ProtocolContractAnalysisFacade(
    lifecycleContext: CangjieAnalysisLifecycleContext,
    private val configuration: ProtocolContractConfiguration = ProtocolContractConfiguration(),
) : AbstractCangjieAnalysisFacade() {
    private val fallbackWorkspaceUri = lifecycleContext.workspaceState.workspaceFolders().firstOrNull()?.uri ?: "file:///workspace"

    val invocations: List<ProtocolInvocation>
        get() = _invocations.toList()

    var lastWorkspaceFolderChange: ProtocolWorkspaceFolderChange? = null
        private set

    override val supportedFeatures: CangjieLspFeatureSet = configuration.supportedFeatures

    private val _invocations = CopyOnWriteArrayList<ProtocolInvocation>()

    fun wasInvoked(name: String): Boolean = _invocations.any { invocation -> invocation.name == name }

    fun invocationCount(name: String): Int = _invocations.count { invocation -> invocation.name == name }

    override fun didOpen(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        record("didOpen", document.uri)
    }

    override fun didChange(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        record("didChange", document.uri)
    }

    override fun didSave(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        record("didSave", document.uri)
    }

    override fun didClose(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        record("didClose", document.uri)
    }

    override fun didChangeWorkspaceFolders(
        context: CangjieAnalysisRequestContext,
        added: List<WorkspaceFolder>,
        removed: List<WorkspaceFolder>,
    ) {
        record("didChangeWorkspaceFolders")
        lastWorkspaceFolderChange = ProtocolWorkspaceFolderChange(
            added = added.map(WorkspaceFolder::getUri),
            removed = removed.map(WorkspaceFolder::getUri),
        )
    }

    override fun didRefreshProjectStructure(context: CangjieAnalysisRequestContext) {
        record("didRefreshProjectStructure")
    }

    override fun collectDiagnostics(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
    ): List<Diagnostic> {
        record("collectDiagnostics", document.uri)
        return listOf(contractDiagnostic(document))
    }

    override fun collectWorkspaceDiagnostics(
        context: CangjieAnalysisRequestContext,
    ): List<WorkspaceDocumentDiagnosticReport> {
        record("collectWorkspaceDiagnostics")
        val openedDocument = context.documentStore.all().firstOrNull()
        val targetUri = openedDocument?.uri ?: "$fallbackWorkspaceUri/contract.cj"
        val diagnostics = openedDocument?.let(::contractDiagnostic)?.let(::listOf).orEmpty()
        return listOf(
            WorkspaceDocumentDiagnosticReport(
                WorkspaceFullDocumentDiagnosticReport(
                    diagnostics,
                    targetUri,
                    openedDocument?.version,
                ),
            ),
        )
    }

    override fun completion(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CompletionParams,
    ): Either<List<CompletionItem>, CompletionList> {
        record("completion", document.uri)
        return Either.forLeft(
            listOf(
                CompletionItem("contractCompletion").apply {
                    kind = CompletionItemKind.Function
                    detail = "protocol contract item"
                },
            ),
        )
    }

    override fun hover(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: HoverParams,
    ): Hover {
        record("hover", document.uri)
        return Hover().apply {
            contents = Either.forRight(
                MarkupContent(MarkupKind.MARKDOWN, "```cangjie\ncontractHover\n```"),
            )
            range = primaryRange(document)
        }
    }

    override fun signatureHelp(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SignatureHelpParams,
    ): SignatureHelp {
        record("signatureHelp", document.uri)
        return SignatureHelp().apply {
            signatures = listOf(
                SignatureInformation("contractSignature(first: Int64, second: Int64)").apply {
                    parameters = listOf(
                        ParameterInformation("first: Int64"),
                        ParameterInformation("second: Int64"),
                    )
                },
            )
            activeSignature = 0
            activeParameter = 1
        }
    }

    override fun declaration(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DeclarationParams,
    ): Either<List<Location>, List<LocationLink>> {
        record("declaration", document.uri)
        return Either.forLeft(listOf(contractLocation(document)))
    }

    override fun definition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DefinitionParams,
    ): Either<List<Location>, List<LocationLink>> {
        record("definition", document.uri)
        return Either.forLeft(listOf(contractLocation(document)))
    }

    override fun typeDefinition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: TypeDefinitionParams,
    ): Either<List<Location>, List<LocationLink>> {
        record("typeDefinition", document.uri)
        return Either.forLeft(listOf(contractLocation(document)))
    }

    override fun implementation(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: ImplementationParams,
    ): Either<List<Location>, List<LocationLink>> {
        record("implementation", document.uri)
        return Either.forLeft(listOf(contractLocation(document)))
    }

    override fun references(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: ReferenceParams,
    ): List<Location> {
        record("references", document.uri)
        return listOf(contractLocation(document))
    }

    override fun documentHighlight(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentHighlightParams,
    ): List<DocumentHighlight> {
        record("documentHighlight", document.uri)
        return listOf(
            DocumentHighlight().apply {
                range = primaryRange(document)
                kind = DocumentHighlightKind.Text
            },
        )
    }

    override fun documentSymbols(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentSymbolParams,
    ): List<Either<SymbolInformation, DocumentSymbol>> {
        record("documentSymbol", document.uri)
        return listOf(
            Either.forRight(
                DocumentSymbol().apply {
                    name = "contractDocumentSymbol"
                    kind = SymbolKind.Function
                    range = fullRange(document)
                    selectionRange = primaryRange(document)
                },
            ),
        )
    }

    override fun workspaceSymbols(
        context: CangjieAnalysisRequestContext,
        params: WorkspaceSymbolParams,
    ): Either<List<SymbolInformation>, List<WorkspaceSymbol>> {
        record("workspaceSymbol")
        return Either.forRight(
            listOf(
                WorkspaceSymbol().apply {
                    name = "contractWorkspaceSymbol"
                    kind = SymbolKind.Class
                    location = Either.forLeft(Location("$fallbackWorkspaceUri/contract.cj", Range(Position(0, 0), Position(0, 4))))
                    containerName = "contract"
                },
            ),
        )
    }

    override fun codeActions(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CodeActionParams,
    ): List<Either<Command, CodeAction>> {
        record("codeAction", document.uri)
        return listOf(
            Either.forRight(
                CodeAction().apply {
                    title = "contractCodeAction"
                    kind = CodeActionKind.QuickFix
                },
            ),
        )
    }

    override fun formatting(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentFormattingParams,
    ): List<TextEdit> {
        record("formatting", document.uri)
        return listOf(TextEdit(primaryRange(document), "contractFormat"))
    }

    override fun rename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): WorkspaceEdit {
        record("rename", document.uri)
        return WorkspaceEdit().apply {
            changes = mapOf(
                document.uri to listOf(TextEdit(primaryRange(document), params.newName)),
            )
        }
    }

    override fun prepareRename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> {
        record("prepareRename", document.uri)
        return Either3.forFirst(primaryRange(document))
    }

    override fun foldingRanges(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: FoldingRangeRequestParams,
    ): List<FoldingRange> {
        record("foldingRange", document.uri)
        return listOf(
            FoldingRange().apply {
                startLine = 0
                endLine = document.text.lineSequence().count().coerceAtLeast(1) - 1
                kind = FoldingRangeKind.Region
            },
        )
    }

    override fun selectionRanges(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SelectionRangeParams,
    ): List<SelectionRange> {
        record("selectionRange", document.uri)
        return params.positions.map {
            SelectionRange().apply {
                range = primaryRange(document)
                parent = SelectionRange().apply {
                    range = fullRange(document)
                }
            }
        }
    }

    override fun semanticTokensFull(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensParams,
    ): SemanticTokens {
        record("semanticTokensFull", document.uri)
        return SemanticTokens(listOf(0, 0, 4, 1, 0))
    }

    override fun semanticTokensRange(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensRangeParams,
    ): SemanticTokens {
        record("semanticTokensRange", document.uri)
        return SemanticTokens(listOf(0, 0, 4, 1, 0))
    }

    override fun inlayHints(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: InlayHintParams,
    ): List<InlayHint> {
        record("inlayHint", document.uri)
        return listOf(
            InlayHint().apply {
                position = Position(0, 0)
                label = Either.forLeft("contractHint")
            },
        )
    }

    private fun record(name: String, documentUri: String? = null) {
        _invocations += ProtocolInvocation(name = name, documentUri = documentUri)
    }

    private fun contractDiagnostic(document: LspTextDocument): Diagnostic {
        return Diagnostic(
            primaryRange(document),
            "contractDiagnostic",
            DiagnosticSeverity.Warning,
            "cangjie",
            "contract",
        )
    }

    private fun contractLocation(document: LspTextDocument): Location {
        return Location(document.uri, primaryRange(document))
    }

    private fun primaryRange(document: LspTextDocument): Range {
        val endOffset = document.text.length.coerceAtMost(4)
        return document.rangeOf(0, endOffset)
    }

    private fun fullRange(document: LspTextDocument): Range {
        return document.rangeOf(0, document.text.length)
    }
}

data class ProtocolContractConfiguration(
    val supportedFeatures: CangjieLspFeatureSet = ProtocolContractFeatures.all(),
)

data class ProtocolInvocation(
    val name: String,
    val documentUri: String? = null,
)

data class ProtocolWorkspaceFolderChange(
    val added: List<String>,
    val removed: List<String>,
)

class ProtocolContractAnalysisFacadeFactory(
    private val configuration: ProtocolContractConfiguration = ProtocolContractConfiguration(),
) {
    @Volatile
    private var facade: ProtocolContractAnalysisFacade? = null

    fun create(context: CangjieAnalysisLifecycleContext): CangjieAnalysisFacade {
        return ProtocolContractAnalysisFacade(context, configuration).also { createdFacade ->
            facade = createdFacade
        }
    }

    fun requireFacade(): ProtocolContractAnalysisFacade {
        return facade ?: error("ProtocolContractAnalysisFacade has not been created yet")
    }
}

object ProtocolContractFeatures {
    fun all(): CangjieLspFeatureSet = CangjieLspFeatureSet(
        completion = true,
        hover = true,
        signatureHelp = true,
        declaration = true,
        definition = true,
        typeDefinition = true,
        implementation = true,
        references = true,
        documentHighlight = true,
        documentSymbol = true,
        workspaceSymbol = true,
        codeAction = true,
        formatting = true,
        rename = true,
        foldingRange = true,
        selectionRange = true,
        semanticTokens = true,
        inlayHints = true,
        diagnostics = true,
    )
}
