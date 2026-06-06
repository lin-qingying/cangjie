

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

    override fun canSeePrivateDeclarationsOfModule(otherModuleData: CfirModuleData): Boolean {
        check(otherModuleData is LLCfirModuleData)
        return otherModuleData.caModule in llCfirSession.caModule.allContextModulesWithSelf
    }

    private val CaModule.allContextModulesWithSelf: Sequence<CaModule>
        get() = generateSequence(this) { if (it is CaDanglingFileModule) it.contextModule else null }

    override fun canSeePrivateTopLevelDeclarationsFromFile(useSiteFile: CfirFile, targetFile: CfirFile): Boolean {
        return useSiteFile.isDanglingFileWithContextFileEqualTo(targetFile)
    }

    private fun CfirFile.isDanglingFileWithContextFileEqualTo(targetFile: CfirFile): Boolean {
        val thisDanglingModule = this.llCfirModuleData.caModule as? CaDanglingFileModule ?: return false
        if (targetFile.llCfirModuleData.caModule is CaDanglingFileModule) return false

        return targetFile.psi in thisDanglingModule.allContextFiles
    }

    private val CaDanglingFileModule.allContextFiles: Sequence<PsiFile>
        get() = allContextModulesWithSelf
            .filterIsInstance<CaDanglingFileModule>()
            .mapNotNull { it.findContextFile() }

    private fun CaDanglingFileModule.findContextFile(): PsiFile? {
        val danglingFile = this.files.singleOrNull() ?: return null
        return when (danglingFile) {
            is CjCodeFragment -> danglingFile.context?.containingFile
            else -> danglingFile.unwrapCopy(danglingFile)
        }
    }
}
