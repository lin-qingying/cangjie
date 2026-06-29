
@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

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
    /**
     * 用于校验待构建文件实际所属模块的项目结构 provider。
     */
    private val projectStructureProvider by lazy { CangJieProjectStructureProvider.getInstance(moduleComponents.session.project) }

    /**
     * 构建或复用指定 PSI 文件的 raw CFIR 文件，并校验 contextual module 与实际文件模块一致。
     */
    fun buildRawCfirFileWithCaching(cjFile: CjFile): CfirFile = moduleComponents.cache.fileCached(cjFile) {
        val contextualModule = moduleComponents.module
        val actualFileModule = projectStructureProvider.getModule(cjFile, contextualModule)

        checkWithAttachment(actualFileModule == contextualModule, { "Modules are inconsistent" }) {
            withEntry("file", cjFile.name)
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
        ).buildCfirFile(cjFile)
    }
}
