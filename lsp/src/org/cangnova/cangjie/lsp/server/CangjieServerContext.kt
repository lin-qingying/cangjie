package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.CangjieLspEnvironment
import org.cangnova.cangjie.lsp.analysis.AnalysisApiLspProjectStructureState
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisLifecycleContext
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisRequestContext
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.state.LspDocumentStore
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.services.LanguageClient

/**
 * LSP 进程内的统一运行时上下文。
 *
 * 这里集中持有环境、workspace/document 状态、Analysis facade 和请求执行器，
 * 并把 diagnostics、project structure、request context 这些平台级公共能力收口为单一协议入口。
 */
class CangjieServerContext(
    val descriptor: CangjieLanguageServerDescriptor,
    val environment: CangjieLspEnvironment,
    val documentStore: LspDocumentStore = LspDocumentStore(),
    val workspaceState: LspWorkspaceState = LspWorkspaceState(),
    val requestExecutor: CangjieRequestExecutor = CangjieRequestExecutor(),
    analysisFacadeFactory: (CangjieAnalysisLifecycleContext) -> CangjieAnalysisFacade,
) : AutoCloseable {
    @Volatile
    var client: LanguageClient? = null

    val analysisFacade: CangjieAnalysisFacade = analysisFacadeFactory(
        CangjieAnalysisLifecycleContext(
            environment = environment,
            descriptor = descriptor,
            workspaceState = workspaceState,
            documentStore = documentStore,
        ),
    )

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

    fun requestContext(): CangjieAnalysisRequestContext {
        return CangjieAnalysisRequestContext(
            environment = environment,
            descriptor = descriptor,
            workspaceState = workspaceState,
            documentStore = documentStore,
        )
    }

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
        return analysisFacade.collectDiagnostics(requestContext(), document)
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

    /**
     * 统一重发当前所有打开文档的诊断。
     *
     * LSP 工作区 overlay 下，某个文件的变更会影响同模块内其他打开文件；
     * 因此诊断刷新必须以“所有打开文档”为单位，而不能只重发当前文档。
     */
    fun republishOpenDiagnostics() {
        if (!enabledFeatures.diagnostics) return

        documentStore.all().forEach { document ->
            requestExecutor.compute {
                val diagnostics = collectDiagnostics(document)
                publishDiagnostics(document, diagnostics)
            }.join()
        }
    }

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

    override fun close() {
        analysisFacade.close()
        requestExecutor.close()
        environment.close()
    }
}
