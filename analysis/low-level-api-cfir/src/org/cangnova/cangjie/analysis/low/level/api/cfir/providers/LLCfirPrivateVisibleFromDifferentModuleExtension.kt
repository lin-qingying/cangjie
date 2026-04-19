/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
        return otherModuleData.ktModule in llCfirSession.ktModule.allContextModulesWithSelf
    }

    private val CaModule.allContextModulesWithSelf: Sequence<CaModule>
        get() = generateSequence(this) { if (it is CaDanglingFileModule) it.contextModule else null }

    override fun canSeePrivateTopLevelDeclarationsFromFile(useSiteFile: CfirFile, targetFile: CfirFile): Boolean {
        return useSiteFile.isDanglingFileWithContextFileEqualTo(targetFile)
    }

    private fun CfirFile.isDanglingFileWithContextFileEqualTo(targetFile: CfirFile): Boolean {
        val thisDanglingModule = this.llCfirModuleData.ktModule as? CaDanglingFileModule ?: return false
        if (targetFile.llCfirModuleData.ktModule is CaDanglingFileModule) return false

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
