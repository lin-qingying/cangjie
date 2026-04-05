package org.cangnova.cangjie.lsp.analysis

import com.intellij.psi.PsiFileFactory
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
    private val lifecycleContext: CangjieAnalysisLifecycleContext,
) {
    private val projectStructureState: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(lifecycleContext.environment.project)

    fun createAnalyzableSnapshot(document: LspTextDocument): AnalysisApiPsiSnapshot {
        val fileName = document.uri.toPsiFileName()
        val psiFile = PsiFileFactory.getInstance(lifecycleContext.environment.project).createFileFromText(
            fileName,
            CangJieFileType.INSTANCE,
            document.analysisText,
        )

        val cangjieFile = psiFile as? CjFile
            ?: error("Expected a Cangjie PSI file for `${document.uri}`, but got `${psiFile::class.qualifiedName}`")

        val useSiteModule = projectStructureState.registerSnapshot(document, cangjieFile)
        return AnalysisApiPsiSnapshot(
            useSiteModule = useSiteModule,
            psiFile = cangjieFile,
        )
    }

    private fun String.toPsiFileName(): String {
        // 保持稳定文件名，便于诊断、源码导航和 project-structure 关联同一路径来源。
        val path = runCatching { URI(this).path }.getOrNull().orEmpty()
        val lastSegment = path.substringAfterLast('/', missingDelimiterValue = path).substringAfterLast('\\')
        val normalized = lastSegment.ifBlank { "untitled.cj" }
        return if (normalized.contains('.')) normalized else "$normalized.cj"
    }
}

internal data class AnalysisApiPsiSnapshot(
    val useSiteModule: CaLspDanglingFileModule,
    val psiFile: CjFile,
)
