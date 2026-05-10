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
    override fun decompile(file: VirtualFile): CharSequence {
        val project = ProjectLocator.getInstance().guessProjectForFile(file)
            ?: ProjectLocator.getPreferredProject(file)
            ?: return ""

        if (project.isDisposed) return ""

        return PsiManager.getInstance(project).findFile(file)?.text.orEmpty()
    }
}
