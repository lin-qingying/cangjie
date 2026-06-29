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
    /**
     * 当前契约测试使用的能力配置，决定 facade 暴露哪些 LSP 功能入口。
     */
    private val configuration: ProtocolContractConfiguration = ProtocolContractConfiguration(),
) : AbstractCangjieAnalysisFacade() {
    /**
     * 工作区为空时用于构造稳定测试 URI 的兜底根路径。
     */
    private val fallbackWorkspaceUri = lifecycleContext.workspaceState.workspaceFolders().firstOrNull()?.uri ?: "file:///workspace"

    /**
     * 已记录的协议入口调用快照，用于测试断言请求是否抵达 facade。
     */
    val invocations: List<ProtocolInvocation>
        get() = _invocations.toList()

    /**
     * 最近一次工作区文件夹变更的归一化记录。
     */
    var lastWorkspaceFolderChange: ProtocolWorkspaceFolderChange? = null
        private set

    /**
     * facade 对外声明支持的 LSP 功能集合。
     */
    override val supportedFeatures: CangjieLspFeatureSet = configuration.supportedFeatures

    /**
     * 线程安全的调用轨迹存储，适配 JSON-RPC 测试中可能出现的异步调用。
     */
    private val _invocations = CopyOnWriteArrayList<ProtocolInvocation>()

    /**
     * 判断指定协议入口是否至少被调用过一次。
     */
    fun wasInvoked(name: String): Boolean = _invocations.any { invocation -> invocation.name == name }

    /**
     * 统计指定协议入口在当前测试会话中的调用次数。
     */
    fun invocationCount(name: String): Int = _invocations.count { invocation -> invocation.name == name }

    /**
     * 记录文档打开通知，验证 textDocument/didOpen 能穿过服务层。
     */
    override fun didOpen(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        record("didOpen", document.uri)
    }

    /**
     * 记录文档变更通知，验证增量或全量同步事件能抵达分析层。
     */
    override fun didChange(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        record("didChange", document.uri)
    }

    /**
     * 记录文档保存通知，验证服务端生命周期事件转发路径。
     */
    override fun didSave(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        record("didSave", document.uri)
    }

    /**
     * 记录文档关闭通知，验证文档从 overlay 移除前后的协议事件。
     */
    override fun didClose(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        record("didClose", document.uri)
    }

    /**
     * 记录工作区文件夹变更，并保留 added/removed URI 供测试精确断言。
     */
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

    /**
     * 记录项目结构刷新通知，验证 workspace 级重载请求的转发。
     */
    override fun didRefreshProjectStructure(context: CangjieAnalysisRequestContext) {
        record("didRefreshProjectStructure")
    }

    /**
     * 为单文档诊断请求返回稳定诊断，便于断言诊断管线的协议封装。
     */
    override fun collectDiagnostics(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
    ): List<Diagnostic> {
        record("collectDiagnostics", document.uri)
        return listOf(contractDiagnostic(document))
    }

    /**
     * 为工作区诊断请求返回稳定报告，优先使用已打开文档，否则构造工作区内固定 URI。
     */
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

    /**
     * 返回固定补全项，验证 completion capability 与 JSON-RPC 结果编码。
     */
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

    /**
     * 返回固定悬停内容和范围，验证 hover 请求的 range 与 markup 传输。
     */
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

    /**
     * 返回固定签名帮助，验证签名列表、当前签名和当前参数的协议映射。
     */
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

    /**
     * 返回固定声明位置，验证 declaration 请求在开启能力时的转发。
     */
    override fun declaration(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DeclarationParams,
    ): Either<List<Location>, List<LocationLink>> {
        record("declaration", document.uri)
        return Either.forLeft(listOf(contractLocation(document)))
    }

    /**
     * 返回固定定义位置，验证 definition 请求的基础导航契约。
     */
    override fun definition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DefinitionParams,
    ): Either<List<Location>, List<LocationLink>> {
        record("definition", document.uri)
        return Either.forLeft(listOf(contractLocation(document)))
    }

    /**
     * 返回固定类型定义位置，验证 typeDefinition 请求的导航结果编码。
     */
    override fun typeDefinition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: TypeDefinitionParams,
    ): Either<List<Location>, List<LocationLink>> {
        record("typeDefinition", document.uri)
        return Either.forLeft(listOf(contractLocation(document)))
    }

    /**
     * 返回固定实现位置，验证 implementation 请求的导航结果编码。
     */
    override fun implementation(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: ImplementationParams,
    ): Either<List<Location>, List<LocationLink>> {
        record("implementation", document.uri)
        return Either.forLeft(listOf(contractLocation(document)))
    }

    /**
     * 返回固定引用位置，验证 references 请求的列表结果契约。
     */
    override fun references(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: ReferenceParams,
    ): List<Location> {
        record("references", document.uri)
        return listOf(contractLocation(document))
    }

    /**
     * 返回固定文档高亮范围，验证 documentHighlight 的类型和范围封装。
     */
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

    /**
     * 返回固定文档符号，验证层级符号响应的范围与选择范围。
     */
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

    /**
     * 返回固定工作区符号，验证 workspace/symbol 的跨文档位置封装。
     */
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

    /**
     * 返回固定代码操作，验证 codeAction capability 与 quick fix 编码。
     */
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

    /**
     * 返回固定格式化编辑，验证 formatting 请求的 TextEdit 响应路径。
     */
    override fun formatting(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentFormattingParams,
    ): List<TextEdit> {
        record("formatting", document.uri)
        return listOf(TextEdit(primaryRange(document), "contractFormat"))
    }

    /**
     * 返回固定重命名编辑，验证 rename 请求生成 WorkspaceEdit 的协议契约。
     */
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

    /**
     * 返回可重命名的固定范围，验证 prepareRename 的三分支结果编码。
     */
    override fun prepareRename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> {
        record("prepareRename", document.uri)
        return Either3.forFirst(primaryRange(document))
    }

    /**
     * 返回覆盖全文的折叠区域，验证 foldingRange 请求对文档行数的处理。
     */
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

    /**
     * 为每个请求位置返回固定选择范围链，验证 selectionRange 的 parent 结构。
     */
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

    /**
     * 返回固定全量语义令牌，验证 semanticTokens/full 的整数编码。
     */
    override fun semanticTokensFull(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensParams,
    ): SemanticTokens {
        record("semanticTokensFull", document.uri)
        return SemanticTokens(listOf(0, 0, 4, 1, 0))
    }

    /**
     * 返回固定范围语义令牌，验证 semanticTokens/range 的整数编码。
     */
    override fun semanticTokensRange(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensRangeParams,
    ): SemanticTokens {
        record("semanticTokensRange", document.uri)
        return SemanticTokens(listOf(0, 0, 4, 1, 0))
    }

    /**
     * 返回固定内联提示，验证 inlayHint 请求的标签和位置封装。
     */
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

    /**
     * 记录一次协议入口调用，统一保存入口名称和可选文档 URI。
     */
    private fun record(name: String, documentUri: String? = null) {
        _invocations += ProtocolInvocation(name = name, documentUri = documentUri)
    }

    /**
     * 构造稳定诊断对象，供单文档和工作区诊断测试复用。
     */
    private fun contractDiagnostic(document: LspTextDocument): Diagnostic {
        return Diagnostic(
            primaryRange(document),
            "contractDiagnostic",
            DiagnosticSeverity.Warning,
            "cangjie",
            "contract",
        )
    }

    /**
     * 构造指向当前文档固定主范围的位置对象。
     */
    private fun contractLocation(document: LspTextDocument): Location {
        return Location(document.uri, primaryRange(document))
    }

    /**
     * 计算契约测试使用的主范围，最多覆盖文档开头四个字符。
     */
    private fun primaryRange(document: LspTextDocument): Range {
        val endOffset = document.text.length.coerceAtMost(4)
        return document.rangeOf(0, endOffset)
    }

    /**
     * 计算覆盖整个测试文档的范围。
     */
    private fun fullRange(document: LspTextDocument): Range {
        return document.rangeOf(0, document.text.length)
    }
}

/**
 * 协议契约 facade 的配置对象，集中定义测试会话需要开启的功能集合。
 */
data class ProtocolContractConfiguration(
    /**
     * 当前测试声明支持的 LSP 功能集合。
     */
    val supportedFeatures: CangjieLspFeatureSet = ProtocolContractFeatures.all(),
)

/**
 * 单次协议入口调用记录。
 */
data class ProtocolInvocation(
    /**
     * 被调用的 facade 入口名称。
     */
    val name: String,
    /**
     * 调用关联的文档 URI；工作区级请求没有文档时为 null。
     */
    val documentUri: String? = null,
)

/**
 * 工作区文件夹变更通知的可断言快照。
 */
data class ProtocolWorkspaceFolderChange(
    /**
     * 本次通知新增的工作区文件夹 URI。
     */
    val added: List<String>,
    /**
     * 本次通知移除的工作区文件夹 URI。
     */
    val removed: List<String>,
)

/**
 * 协议契约测试使用的 AnalysisFacade 工厂。
 */
class ProtocolContractAnalysisFacadeFactory(
    /**
     * 创建 facade 时注入的契约测试配置。
     */
    private val configuration: ProtocolContractConfiguration = ProtocolContractConfiguration(),
) {
    /**
     * 最近创建的 facade 实例，供测试在服务启动后读取调用轨迹。
     */
    @Volatile
    private var facade: ProtocolContractAnalysisFacade? = null

    /**
     * 创建新的契约 facade，并保存实例引用供测试断言。
     */
    fun create(context: CangjieAnalysisLifecycleContext): CangjieAnalysisFacade {
        return ProtocolContractAnalysisFacade(context, configuration).also { createdFacade ->
            facade = createdFacade
        }
    }

    /**
     * 返回已创建的 facade；若服务尚未启动则报告测试夹具使用错误。
     */
    fun requireFacade(): ProtocolContractAnalysisFacade {
        return facade ?: error("ProtocolContractAnalysisFacade has not been created yet")
    }
}

/**
 * 协议契约测试的能力集合构造器。
 */
object ProtocolContractFeatures {
    /**
     * 构造开启全部当前 LSP 功能的能力集合。
     */
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
