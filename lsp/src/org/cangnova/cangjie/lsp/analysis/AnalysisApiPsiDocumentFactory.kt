package org.cangnova.cangjie.lsp.analysis

import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.psi.CjFile
import java.net.URI

/**
 * 从 LSP 文档快照构建 Analysis API 可消费的 PSI 快照。
 *
 * 这里必须明确区分两层文本语义：
 * 1. LSP 文档存储保留客户端原始文本，用于协议层版本与增量编辑；
 * 2. PSI 快照始终使用 `\n` 规范化文本，保证 Analysis API 的 offset 语义稳定。
 *
 * 同时，该工厂负责把 PSI 快照注册回 LSP 项目结构状态，使 snapshot use-site module
 * 与当前文档版本保持一致。
 */
internal class AnalysisApiPsiDocumentFactory(
    /**
     * 创建 PSI 快照所需的 LSP 分析生命周期上下文。
     */
    private val lifecycleContext: CangjieAnalysisLifecycleContext,
) {
    /**
     * 当前 project 中维护的 LSP 项目结构状态。
     *
     * 该状态保存打开文档快照与 use-site module 的映射。
     */
    private val projectStructureState: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(lifecycleContext.environment.project)

    /**
     * 统一维护打开文档对应的 PSI 快照。
     *
     * 同一版本的文档会复用已有 PSI，避免语义查询再额外制造一次性快照。
     */
    fun upsertSnapshot(document: LspTextDocument): CjFile {
        projectStructureState.openDocumentSnapshot(document.uri)?.let { snapshot ->
            if (snapshot.document.version == document.version && snapshot.document.text == document.text) {
                return snapshot.psiFile
            }
        }

        val psiFile = createPsiFile(document)
        projectStructureState.upsertOpenDocumentSnapshot(document, psiFile)
        return psiFile
    }

    /**
     * 移除指定 URI 对应的打开文档 PSI 快照。
     *
     * 文档关闭时调用该方法，避免旧版本 overlay PSI 继续参与项目结构计算。
     */
    fun removeSnapshot(uri: String) {
        projectStructureState.removeOpenDocumentSnapshot(uri)
    }

    /**
     * 创建可直接进入 Analysis API 的 PSI 快照。
     *
     * 返回值同时携带 PSI 文件和其 use-site module，确保后续语义查询具备正确模块上下文。
     */
    fun createAnalyzableSnapshot(document: LspTextDocument): AnalysisApiPsiSnapshot {
        val cangjieFile = upsertSnapshot(document)
        val useSiteModule = projectStructureState.useSiteModuleForOpenDocument(document.uri)
            ?: error(
                "LSP document `${document.uri}` 尚未绑定到 use-site 模块。" +
                    "请先完成 snapshot 更新并刷新 project structure。",
            )
        return AnalysisApiPsiSnapshot(
            useSiteModule = useSiteModule,
            psiFile = cangjieFile,
        )
    }

    /**
     * 为 LSP 文档构造仓颉 PSI 文件。
     *
     * 文件名从 URI 稳定推导，文本使用 analysis 规范化版本。
     */
    private fun createPsiFile(document: LspTextDocument): CjFile {
        val fileName = document.uri.toPsiFileName()
        return LspAnalysisPsiFileFactory.createFile(
            project = lifecycleContext.environment.project,
            documentUri = document.uri,
            fileName = fileName,
            text = document.analysisText,
        )
    }

    /**
     * 将 LSP 文档 URI 转换为 PSI 文件名。
     *
     * URI 没有文件扩展名时补充 `.cj`，无法推导时使用 `untitled.cj`。
     */
    private fun String.toPsiFileName(): String {
        // 保持稳定文件名，便于诊断、源码导航和 project-structure 关联同一路径来源。
        val path = runCatching { URI(this).path }.getOrNull().orEmpty()
        val lastSegment = path.substringAfterLast('/', missingDelimiterValue = path).substringAfterLast('\\')
        val normalized = lastSegment.ifBlank { "untitled.cj" }
        return if (normalized.contains('.')) normalized else "$normalized.cj"
    }
}

/**
 * Analysis API 可消费的 LSP PSI 快照。
 *
 * 快照把当前 PSI 文件和它所属的 use-site module 绑定在一起，作为单次语义分析的输入。
 */
internal data class AnalysisApiPsiSnapshot(
    /**
     * 当前 PSI 文件用于语义分析的 use-site 模块。
     */
    val useSiteModule: CaModule,

    /**
     * 当前 LSP 文档版本对应的仓颉 PSI 文件。
     */
    val psiFile: CjFile,
)
