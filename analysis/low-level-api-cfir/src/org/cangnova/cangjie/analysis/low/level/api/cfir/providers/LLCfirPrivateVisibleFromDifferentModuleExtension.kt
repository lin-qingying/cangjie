

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.CfirPrivateVisibleFromDifferentModuleExtension
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.unwrapCopy
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.psi.CjCodeFragment

/**
 * [CfirPrivateVisibleFromDifferentModuleExtension] which is aware of [CaDanglingFileModule]s.
 * Dangling file can see private declarations of its respective context file.
 */
internal class LLCfirPrivateVisibleFromDifferentModuleExtension(private val llCfirSession: LLCfirSession) :
    CfirPrivateVisibleFromDifferentModuleExtension() {

    /**
     * 判断当前 session 模块是否可以访问 [otherModuleData] 所属模块的 private 声明。
     *
     * dangling file module 可以沿 context module 链访问上下文模块的 private 声明。
     */
    override fun canSeePrivateDeclarationsOfModule(otherModuleData: CfirModuleData): Boolean {
        check(otherModuleData is LLCfirModuleData)
        return otherModuleData.caModule in llCfirSession.caModule.allContextModulesWithSelf
    }

    /**
     * 当前模块及其 dangling context module 链。
     */
    private val CaModule.allContextModulesWithSelf: Sequence<CaModule>
        get() = generateSequence(this) { if (it is CaDanglingFileModule) it.contextModule else null }

    /**
     * 判断 [useSiteFile] 是否为 [targetFile] 的 dangling 上下文文件，从而允许访问 private 顶层声明。
     */
    override fun canSeePrivateTopLevelDeclarationsFromFile(useSiteFile: CfirFile, targetFile: CfirFile): Boolean {
        return useSiteFile.isDanglingFileWithContextFileEqualTo(targetFile)
    }

    /**
     * 判断当前 CFIR 文件是否属于 dangling module，且 [targetFile] 是其上下文文件之一。
     */
    private fun CfirFile.isDanglingFileWithContextFileEqualTo(targetFile: CfirFile): Boolean {
        val thisDanglingModule = this.llCfirModuleData.caModule as? CaDanglingFileModule ?: return false
        if (targetFile.llCfirModuleData.caModule is CaDanglingFileModule) return false

        return targetFile.psi in thisDanglingModule.allContextFiles
    }

    /**
     * dangling module 链中所有可解析出的上下文 PSI 文件。
     */
    private val CaDanglingFileModule.allContextFiles: Sequence<PsiFile>
        get() = allContextModulesWithSelf
            .filterIsInstance<CaDanglingFileModule>()
            .mapNotNull { it.findContextFile() }

    /**
     * 从 dangling module 的单个文件中恢复其上下文 PSI 文件。
     *
     * 代码片段使用 context；普通 copied 文件通过 [unwrapCopy] 回到原文件。
     */
    private fun CaDanglingFileModule.findContextFile(): PsiFile? {
        val danglingFile = this.files.singleOrNull() ?: return null
        return when (danglingFile) {
            is CjCodeFragment -> danglingFile.context?.containingFile
            else -> danglingFile.unwrapCopy(danglingFile)
        }
    }
}
