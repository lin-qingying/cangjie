
@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.MacroSurfaceCollectionMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistryOrNull
import org.cangnova.cangjie.cfir.session.recordRawMacroSurfaces
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
        buildRawCfirFile(cjFile)
    }

    /**
     * 为需要变换 CFIR 树的离线构造阶段创建独立 raw 文件，不接触解析缓存中的共享树。
     *
     * 宏 stable splicer 会原地更新 CFIR 节点；宏构造输入若复用普通 LL 查询缓存，
     * 则构造尚未发布时就会改变其他读者可见的语义。本入口保持相同 builder 与 surface 收集，
     * 但每次都返回新建的文件树，供构造快照独占使用。
     */
    fun buildRawCfirFileUncached(cjFile: CjFile): CfirFile = buildRawCfirFile(
        cjFile,
        macroSurfaceCollectionMode = MacroSurfaceCollectionMode.CONSTRUCTION,
    )

    /**
     * 使用当前模块解析上下文构建一棵 raw CFIR 树并记录该树对应的宏 surface。
     */
    private fun buildRawCfirFile(
        cjFile: CjFile,
        macroSurfaceCollectionMode: MacroSurfaceCollectionMode = currentMacroSurfaceCollectionMode(),
    ): CfirFile {
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

        val builder = PsiRawCfirBuilder(
            moduleComponents.session,
            moduleComponents.scopeProvider,
            bodyBuildingMode = BodyBuildingMode.LAZY_BODIES,
            macroSurfaceCollectionMode = macroSurfaceCollectionMode,
        )
        val cfirFile = builder.buildCfirFile(cjFile)
        moduleComponents.session.recordRawMacroSurfaces(
            file = cfirFile,
            sourceFileIdentity = cjFile,
            surfaces = builder.consumeCollectedMacroSurfaces(),
        )
        return cfirFile
    }

    /** 已冻结的 construction snapshot 是分析期只读事实，缓存重建不得登记新的 annotation slot。 */
    private fun currentMacroSurfaceCollectionMode(): MacroSurfaceCollectionMode =
        if (moduleComponents.session.annotationMetadataRegistryOrNull?.isFrozen == true) {
            MacroSurfaceCollectionMode.EXPRESSION_ONLY
        } else {
            MacroSurfaceCollectionMode.CONSTRUCTION
        }
}
