package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.CangjieLspEnvironment
import org.cangnova.cangjie.lsp.analysis.AnalysisApiLspProjectStructureState
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisLifecycleContext
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisRequestContext
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.completion.LspCompletionSessionStore
import org.cangnova.cangjie.lsp.state.LspDocumentStore
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.DiagnosticCollectionFailures
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.WorkspaceDocumentDiagnosticReport
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient

/**
 * LSP 进程内的统一运行时上下文。
 *
 * 这里集中持有环境、workspace/document 状态、Analysis facade 和请求执行器，
 * 并把 diagnostics、project structure、request context 这些平台级公共能力收口为单一协议入口。
 */
class CangjieServerContext(
    /**
     * 当前服务端的静态能力描述。
     */
    val descriptor: CangjieLanguageServerDescriptor,

    /**
     * 当前 LSP 会话绑定的编译器和 IntelliJ 运行环境。
     */
    val environment: CangjieLspEnvironment,

    /**
     * 当前打开文档的协议层快照存储。
     */
    val documentStore: LspDocumentStore = LspDocumentStore(),

    /**
     * 补全会话存储：候选与替换区间要保留到客户端接受候选那一刻（有上界，超限即过期）。
     */
    val completionSessions: LspCompletionSessionStore = LspCompletionSessionStore(),

    /**
     * 当前工作区初始化参数、目录和项目配置状态。
     */
    val workspaceState: LspWorkspaceState = LspWorkspaceState(),

    /**
     * 串行执行语义请求的请求执行器。
     */
    val requestExecutor: CangjieRequestExecutor = CangjieRequestExecutor(
        statistics = CangjieRequestStatistics.forProject(environment.project),
    ),
    /** 测试宿主保留 isolated failure 供外部 drain；生产请求由诊断隔离边界记录日志后释放异常引用。 */
    private val retainDiagnosticCollectionFailures: Boolean = false,
    analysisFacadeFactory: (CangjieAnalysisLifecycleContext) -> CangjieAnalysisFacade,
) : AutoCloseable {
    /**
     * 当前连接的 LSP 客户端代理。
     *
     * 初始化完成后由语言服务器写入，诊断发布等 server-to-client 通知通过该代理发送。
     */
    @Volatile
    var client: LanguageClient? = null

    /** 未打开源码文件上由宏 construction 诊断占用的 push URI。 */
    private val publishedMacroDiagnosticUris = linkedSetOf<String>()

    /** 串行化宏快照变更后的全工作区诊断重算与 URI 清除。 */
    private val macroDiagnosticPublicationLock = Any()

    /**
     * 当前服务端使用的语义分析 facade。
     *
     * facade 根据生命周期上下文创建，负责连接协议请求与 Analysis API 或测试替身。
     */
    val analysisFacade: CangjieAnalysisFacade = analysisFacadeFactory(
        CangjieAnalysisLifecycleContext(
            environment = environment,
            descriptor = descriptor,
            workspaceState = workspaceState,
            documentStore = documentStore,
            completionSessions = completionSessions,
        ),
    )

    /**
     * 当前真正启用的 LSP 功能集合。
     *
     * 该集合是静态 descriptor 能力与 analysis facade 支持能力的交集。
     */
    val enabledFeatures: CangjieLspFeatureSet
        get() = descriptor.features.intersect(analysisFacade.supportedFeatures)

    /**
     * LSP 侧的项目结构状态是 Analysis API 平台服务的唯一事实来源。
     *
     * 文档事件、工作区事件与语义请求都必须围绕同一份状态刷新和查询，
     * 不能各自拼装“模块图”或“可见文件集合”。
     */
    val projectStructureState: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(environment.project)

    /**
     * 构造一次请求所需的 Analysis 上下文。
     *
     * 请求上下文使用当前 workspace/document 状态，确保每个协议请求看到最新快照。
     */
    fun requestContext(): CangjieAnalysisRequestContext {
        return CangjieAnalysisRequestContext(
            environment = environment,
            descriptor = descriptor,
            workspaceState = workspaceState,
            documentStore = documentStore,
        )
    }

    /** 使用当前宿主的保留策略包围一个诊断批次。 */
    private fun <T> withDiagnosticCollection(block: () -> T): T =
        DiagnosticCollectionFailures.withCollection(
            retainFailures = retainDiagnosticCollectionFailures,
            block = block,
        )

    /**
     * 带取消状态的请求上下文。
     *
     * 需要支持 `$/cancelRequest` 的路径（补全）用这个重载：取消状态必须随请求一起
     * 进到管线里，否则客户端取消后语义工作仍会跑完。
     */
    fun requestContext(cancellation: LspRequestCancellation): CangjieAnalysisRequestContext =
        requestContext().copy(cancellation = cancellation)

    /**
     * 统一刷新 LSP 平台层的项目结构快照。
     *
     * 文档打开、修改、保存、关闭，以及工作区目录或磁盘文件变化，都必须先更新这份结构状态，
     * 然后再进入 Analysis API 语义层。
     */
    fun refreshProjectStructure() {
        projectStructureState.configure(workspaceState = workspaceState)
    }

    /**
     * 统一收口单文档诊断收集。
     *
     * 所有 push / pull diagnostics 都必须复用这一入口，避免服务层各自重新拼 request context、
     * feature 开关和 Analysis facade 调用顺序。
     */
    fun collectDiagnostics(document: LspTextDocument): List<Diagnostic> {
        return withDiagnosticCollection {
            analysisFacade.collectDiagnostics(requestContext(), document)
        }
    }

    /** 收集单个 codeAction 请求的诊断与 quick-fix 候选。 */
    fun codeActions(
        document: LspTextDocument,
        params: CodeActionParams,
    ): List<Either<Command, CodeAction>> = withDiagnosticCollection {
        analysisFacade.codeActions(requestContext(), document, params)
    }

    /** 在当前宿主的 failure-retention 策略下解析一个 codeAction。 */
    fun resolveCodeAction(
        document: LspTextDocument,
        params: CodeAction,
    ): CodeAction = withDiagnosticCollection {
        analysisFacade.resolveCodeAction(requestContext(), document, params)
    }

    /** 在一个共享批次中收集完整工作区，避免逐文件重置切断本轮 failure capture。 */
    fun collectWorkspaceDiagnostics(): List<WorkspaceDocumentDiagnosticReport> =
        withDiagnosticCollection {
            analysisFacade.collectWorkspaceDiagnostics(requestContext())
        }

    /** 消费工作区 pull diagnostics 的统一收集入口。 */
    fun collectWorkspaceDiagnostics(context: CangjieAnalysisRequestContext): List<WorkspaceDocumentDiagnosticReport> =
        withDiagnosticCollection {
            analysisFacade.collectWorkspaceDiagnostics(context)
        }

    /**
     * 统一构造并发布单文档诊断通知。
     *
     * 文本服务、工作区服务和平台刷新逻辑都只能通过这一入口向客户端发布 push diagnostics，
     * 保证 version 字段、URI 和诊断列表的构造语义一致。
     */
    fun publishDiagnostics(
        document: LspTextDocument,
        diagnostics: List<Diagnostic>,
    ) {
        val activeClient = client ?: return
        activeClient.publishDiagnostics(createPublishDiagnosticsParams(document, diagnostics))
    }

    /** 发布未打开源码文件的诊断；这些通知没有文档版本。 */
    fun publishDiagnostics(uri: String, diagnostics: List<Diagnostic>) {
        val activeClient = client ?: return
        activeClient.publishDiagnostics(PublishDiagnosticsParams(uri, diagnostics))
    }

    /**
     * 统一重发当前所有打开文档的诊断。
     *
     * LSP 工作区 overlay 下，某个文件的变更会影响同模块内其他打开文件；
     * 因此诊断刷新必须以“所有打开文档”为单位，而不能只重发当前文档。
     */
    fun republishOpenDiagnostics() {
        if (!enabledFeatures.diagnostics) return

        requestExecutor.compute(CangjieLspRequest.PUBLISH_DIAGNOSTICS) {
            withDiagnosticCollection {
                republishOpenDiagnosticsOnCurrentThread()
            }
        }.join()
    }

    /** 在当前串行 LSP worker 上重发所有已打开文档的完整诊断。 */
    private fun republishOpenDiagnosticsOnCurrentThread() {
        documentStore.all().forEach { document ->
            val diagnostics = collectDiagnostics(document)
            publishDiagnostics(document, diagnostics)
        }
    }

    /**
     * 宏语义快照发布或失效后，刷新打开文档，并向未打开的工作区源码文件发布宏错误。
     *
     * 打开文件与工作区文件在同一收集批次内处理，避免不同文件的 isolated failure 相互清空。
     * 下一轮快照不再含宏错误时会向先前 URI 发送空列表清除旧诊断。
     */
    fun republishMacroDiagnostics() {
        if (!enabledFeatures.diagnostics) return

        synchronized(macroDiagnosticPublicationLock) {
            val reports = requestExecutor.compute(CangjieLspRequest.WORKSPACE_DIAGNOSTIC) {
                withDiagnosticCollection {
                    republishOpenDiagnosticsOnCurrentThread()
                    collectWorkspaceDiagnostics()
                }
            }.join()
            val openUris = documentStore.all().mapTo(hashSetOf()) { it.uri }
            val currentDiagnostics = reports.mapNotNull { report ->
                if (!report.isWorkspaceFullDocumentDiagnosticReport) return@mapNotNull null
                val fullReport = report.workspaceFullDocumentDiagnosticReport
                if (fullReport.uri in openUris) return@mapNotNull null

                val macroDiagnostics = fullReport.items.filter { diagnostic ->
                    diagnostic.severity == org.eclipse.lsp4j.DiagnosticSeverity.Error &&
                        diagnostic.isMacroConstructionDiagnostic()
                }
                macroDiagnostics.takeIf { it.isNotEmpty() }?.let { fullReport.uri to it }
            }.toMap()

            val staleUris = (publishedMacroDiagnosticUris - currentDiagnostics.keys - openUris).toList()
            staleUris.forEach { uri -> publishDiagnostics(uri, emptyList()) }
            currentDiagnostics.forEach { (uri, diagnostics) -> publishDiagnostics(uri, diagnostics) }

            publishedMacroDiagnosticUris.clear()
            publishedMacroDiagnosticUris.addAll(currentDiagnostics.keys)
        }
    }

    /**
     * 构造 publishDiagnostics 通知参数。
     *
     * 该方法按客户端协商决定是否写入 version 字段，避免诊断发布路径各自处理版本兼容逻辑。
     */
    fun createPublishDiagnosticsParams(
        document: LspTextDocument,
        diagnostics: List<Diagnostic>,
    ): PublishDiagnosticsParams {
        return PublishDiagnosticsParams(document.uri, diagnostics).apply {
            if (workspaceState.supportsPublishDiagnosticsVersion()) {
                version = document.version
            }
        }
    }

    /**
     * 关闭 analysis facade、请求执行器和 LSP 环境。
     *
     * 关闭顺序保证上层语义资源先释放，再释放承载它们的执行器和平台环境。
     */
    override fun close() {
        analysisFacade.close()
        requestExecutor.close()
        environment.close()
    }
}

/** LSP diagnostic codes expose the factory name without the generated `CFIR_` namespace. */
private fun Diagnostic.isMacroConstructionDiagnostic(): Boolean =
    code?.left?.let { it.startsWith("MACRO_") || it.startsWith("CFIR_MACRO_") } == true
