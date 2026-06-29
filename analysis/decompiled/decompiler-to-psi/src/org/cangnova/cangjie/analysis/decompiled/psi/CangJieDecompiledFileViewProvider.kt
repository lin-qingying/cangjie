package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.PsiFileImpl
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.utils.concurrent.block.LockedClearableLazyValue

/**
 * 仓颉 `.cjo` 反编译文件的单根 PSI view provider。
 *
 * 该 provider 以 throw-away PSI 文件生成可展示文本，并通过 [factory] 创建真实的
 * [CjDecompiledFile]；这样 IntelliJ 可以像普通源文件一样读取反编译文本和 stub 树。
 */
class CangJieDecompiledFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    physical: Boolean,
    /**
     * 根据当前 view provider 创建具体反编译 PSI 文件的工厂函数。
     */
    private val factory: (CangJieDecompiledFileViewProvider) -> CjDecompiledFile?,
) : SingleRootFileViewProvider(manager, file, physical, CangJieLanguage) {
    /**
     * 懒加载的反编译文本缓存。
     *
     * 文本通过临时 PSI 文件生成，生成后立即将临时 PSI 标记为失效，避免它被误当成真实 PSI 树继续使用。
     */
    val content: LockedClearableLazyValue<String> = LockedClearableLazyValue(Any()) {
        val psiFile = createFile(manager.project, file, CangJieBuiltInFileType)
        val text = psiFile?.text ?: ""

        DebugUtil.performPsiModification<PsiInvalidElementAccessException>("Invalidating throw-away copy of file that was used for getting text") {
            (psiFile as? PsiFileImpl)?.markInvalidated()
        }

        text
    }

    /**
     * 创建当前 `.cjo` 文件对应的反编译 PSI 文件。
     */
    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        return factory(this)
    }

    /**
     * 为 IntelliJ 的 view provider 复制流程创建共享同一 PSI 工厂的副本。
     */
    override fun createCopy(copy: VirtualFile) = CangJieDecompiledFileViewProvider(manager, copy, false, factory)

    /**
     * 返回可供编辑器和导航展示的反编译文本内容。
     */
    override fun getContents(): CharSequence = content.get()
}
