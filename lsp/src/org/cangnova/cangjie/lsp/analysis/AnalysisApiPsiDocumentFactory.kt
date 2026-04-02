package org.cangnova.cangjie.lsp.analysis

import com.intellij.psi.PsiFileFactory
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.psi.CjFile
import java.net.URI

/**
 * 从 LSP 文本文档创建可参与分析的 PSI 快照。
 *
 * `:lsp` 只依赖 Analysis API，但 Analysis API 的公开接口本身已经暴露了 [CjFile]
 * 这样的 PSI 类型，因此把这层桥接集中收口在这里，避免散落到各个请求实现中。
 */
internal class AnalysisApiPsiDocumentFactory(
    private val lifecycleContext: CangjieAnalysisLifecycleContext,
) {
    fun createAnalyzableFile(document: LspTextDocument): CjFile {
        val fileName = document.uri.toPsiFileName()
        val psiFile = PsiFileFactory.getInstance(lifecycleContext.environment.project).createFileFromText(
            fileName,
            CangJieFileType.INSTANCE,
            document.text,
        )

        return psiFile as? CjFile
            ?: error("Expected a Cangjie PSI file for `${document.uri}`, but got `${psiFile::class.qualifiedName}`")
    }

    private fun String.toPsiFileName(): String {
        // 保持稳定文件名，便于诊断和后续基于 PSI 的能力返回可追踪来源。
        val path = runCatching { URI(this).path }.getOrNull().orEmpty()
        val lastSegment = path.substringAfterLast('/', missingDelimiterValue = path).substringAfterLast('\\')
        val normalized = lastSegment.ifBlank { "untitled.cj" }
        return if (normalized.contains('.')) normalized else "$normalized.cj"
    }
}
