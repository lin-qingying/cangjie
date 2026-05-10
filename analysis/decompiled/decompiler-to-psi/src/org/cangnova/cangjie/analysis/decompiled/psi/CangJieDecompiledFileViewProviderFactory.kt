package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager

/**
 * `.cjo` binary file 到仓颉 decompiled PSI 的平台入口。
 *
 * 这里作为 file type 层入口，只委托仓颉自己的 [CjoFileDecompilers] 协议。
 */
class CangJieDecompiledFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language?,
        manager: PsiManager,
        eventSystemEnabled: Boolean,
    ): FileViewProvider {
        val decompiler = checkNotNull(CjoFileDecompilers.getInstance().find(file, CjoFileDecompilers.Full::class.java)) {
            "CangJie .cjo decompiler is not registered for ${file.path}"
        }
        return decompiler.createFileViewProvider(file, manager, eventSystemEnabled)
    }
}
