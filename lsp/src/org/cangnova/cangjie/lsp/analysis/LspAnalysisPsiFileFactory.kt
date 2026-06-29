package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.CjFile
import java.net.URI
import java.nio.file.Path

/**
 * LSP Analysis API 的 PSI 文件工厂。
 *
 * LSP 打开的 overlay 文档和少量磁盘 fallback 文件都必须绑定稳定的 `VirtualFile` 身份：
 * 1. scope 过滤需要真实 path，避免非物理 PSI 被误并入任意模块/库作用域；
 * 2. diagnostics / definition / references 依赖 `VirtualFile` 恢复 use-site 与库边界；
 * 3. 工作区 overlay 需要用真实 URI 挂接到同一份源码身份，而不是匿名内存副本。
 */
internal object LspAnalysisPsiFileFactory {
    /**
     * 为指定 LSP 文档创建仓颉 PSI 文件。
     *
     * 返回的 PSI 绑定自定义 light virtual file，其路径和 URL 与 LSP 文档 URI 保持一致。
     */
    fun createFile(
        project: Project,
        documentUri: String,
        fileName: String,
        text: CharSequence,
    ): CjFile {
        val virtualFile = LspAnalysisVirtualFile(
            documentUri = documentUri,
            fileName = fileName,
            text = text,
        )
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
        return psiFile as? CjFile
            ?: error("Expected a Cangjie PSI file for `$documentUri`, but got `${psiFile?.javaClass?.name}`")
    }
}

/**
 * 为 LSP snapshot 提供稳定 URI/path 的 light virtual file。
 *
 * 默认 `LightVirtualFile` 只暴露临时名称，无法参与基于路径的 scope 判定。
 * 这里显式把 `path/url` 绑定到 LSP 文档 URI，使 overlay PSI 仍然能按真实工作区路径参与分析。
 */
private class LspAnalysisVirtualFile(
    /**
     * LSP 文档的原始 URI。
     */
    private val documentUri: String,
    fileName: String,
    text: CharSequence,
) : LightVirtualFile(fileName, CangJieFileType.INSTANCE, text) {
    /**
     * 从文档 URI 解析得到的本地路径表示。
     *
     * URI 无法转换为标准路径时退回 URI path 或文件名，保证 scope 计算始终有稳定路径。
     */
    private val resolvedPath: String =
        runCatching { Path.of(URI(documentUri)).normalize().toString() }
            .getOrElse {
                runCatching { URI(documentUri).path }.getOrDefault(fileName).ifBlank { fileName }
            }

    /**
     * 返回供 IntelliJ scope 和模块判定使用的稳定路径。
     */
    override fun getPath(): String = resolvedPath

    /**
     * 返回与 LSP 文档身份一致的 URL。
     */
    override fun getUrl(): String = documentUri
}
