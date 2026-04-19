

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.ThreadSafe
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment

/**
 * Responsble for building [CfirFile] by [CjFile]
 */
@ThreadSafe
internal class LLCfirFileBuilder(val moduleComponents: LLCfirModuleResolveComponents) {
    private val projectStructureProvider by lazy { CangJieProjectStructureProvider.getInstance(moduleComponents.session.project) }

    fun buildRawCfirFileWithCaching(ktFile: CjFile): CfirFile = moduleComponents.cache.fileCached(ktFile) {
        val contextualModule = moduleComponents.module
        val actualFileModule = projectStructureProvider.getModule(ktFile, contextualModule)

        checkWithAttachment(actualFileModule == contextualModule, { "Modules are inconsistent" }) {
            withEntry("file", ktFile.name)
            withEntry("file module", actualFileModule) {
                it.toString()
            }
            withEntry("components module", contextualModule) {
                it.toString()
            }
        }

        PsiRawCfirBuilder(
            moduleComponents.session,
            moduleComponents.scopeProvider,
            bodyBuildingMode = BodyBuildingMode.LAZY_BODIES
        ).buildCfirFile(ktFile)
    }
}
