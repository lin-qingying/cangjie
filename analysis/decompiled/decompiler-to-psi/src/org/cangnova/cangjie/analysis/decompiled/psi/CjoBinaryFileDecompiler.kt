package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

/**
 * `.cjo` 二进制文件的编辑器文本入口。
 *
 * 该类只承担 IntelliJ 平台 `filetype.decompiler` 接点职责：
 * 将 binary editor document 的文本请求导回
 * `PsiManager.findFile(...) -> CjDecompiledFile -> compiled stub -> decompiled text`
 * 主链，不单独实现文本渲染路径。
 */
class CjoBinaryFileDecompiler : BinaryFileDecompiler {
    /**
     * 返回 `.cjo` 文件在编辑器中展示的反编译文本。
     *
     * 项目不可解析或已释放时返回空文本；正常路径通过 PSI 管理器触发
     * [CangJieDecompiledFileViewProvider] 和 compiled stub 的既有渲染链路。
     */
    override fun decompile(file: VirtualFile): CharSequence {
        val project = ProjectLocator.getInstance().guessProjectForFile(file)
            ?: ProjectLocator.getPreferredProject(file)
            ?: return ""

        if (project.isDisposed) return ""

        return PsiManager.getInstance(project).findFile(file)?.text.orEmpty()
    }
}
