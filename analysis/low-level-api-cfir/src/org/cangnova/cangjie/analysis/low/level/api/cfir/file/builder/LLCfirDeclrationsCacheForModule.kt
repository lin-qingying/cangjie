/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import com.google.common.collect.MapMaker
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsOnlyApi
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.ThreadSafe
import java.util.concurrent.ConcurrentMap

/**
 * Caches the [CjFile] to [CfirFile] mapping of a [CaModule][org.cangnova.cangjie.analysis.api.projectStructure.CaModule].
 */
@ThreadSafe
internal abstract class ModuleFileCache {
    /**
     * 当前 cache 所属模块的解析组件。
     */
    abstract val moduleComponents: LLCfirModuleResolveComponents

    /**
     * @return [CfirFile] by [file] if it was previously built or runs [createValue] otherwise
     * The [createValue] is run under the lock so [createValue] is executed at most once for each [CjFile]
     */
    abstract fun fileCached(file: CjFile, createValue: () -> CfirFile): CfirFile

    /**
     * 返回包含指定 CFIR 声明的已缓存 CFIR 文件。
     */
    abstract fun getContainerCfirFile(declaration: CfirDeclaration): CfirFile?

    /**
     * 返回指定 PSI 文件已经构建过的 raw CFIR 文件。
     */
    abstract fun getCachedCfirFile(cjFile: CjFile): CfirFile?

    /**
     * 返回当前 low-level session 可参与 lazy resolve 的 CFIR 文件。
     *
     * 宏语义快照发布后，解析阶段使用快照中的 expanded 文件；未发布快照时使用已构建的 raw 文件。
     */
    abstract fun getAllCachedCfirFilesForResolution(): Collection<CfirFile>

    @LLStatisticsOnlyApi
    /**
     * 返回统计 API 可见的所有已缓存 CFIR 文件。
     */
    fun getAllCachedCfirFiles(): Collection<CfirFile> = getAllCachedCfirFilesForResolution()
}

/**
 * 基于弱 key map 的模块文件 cache 实现。
 */
internal class ModuleFileCacheImpl(override val moduleComponents: LLCfirModuleResolveComponents) : ModuleFileCache() {
    /**
     * 物理 PSI 文件到 raw CFIR 文件的弱 key 缓存。
     */
    private val cjFileToCfirFile: ConcurrentMap<CjFile, CfirFile> = MapMaker().weakKeys().makeMap()

    /**
     * 返回当前语义视图中的 PSI 文件，缺失时构建并缓存 raw CFIR。
     *
     * 宏构造仍使用独立的未缓存树；完整快照发布后，普通 PSI 查询必须通过源文件身份映射到同一份 expanded CFIR。
     * 否则文件 checker 会把缓存中的 pre-macro 声明与 package scope 中快照的同一声明比较成跨文件重载冲突。
     */
    override fun fileCached(file: CjFile, createValue: () -> CfirFile): CfirFile =
        moduleComponents.macroSemanticSnapshotStore.snapshot?.fileForSource(file)
            ?: cjFileToCfirFile.computeIfAbsent(file) { createValue() }

    /**
     * 查询当前语义视图中已构建的文件；宏快照发布后优先返回对应 expanded 文件。
     */
    override fun getCachedCfirFile(cjFile: CjFile): CfirFile? =
        moduleComponents.macroSemanticSnapshotStore.snapshot?.fileForSource(cjFile)
            ?: cjFileToCfirFile[cjFile]

    /**
     * 通过声明 PSI 所在文件定位包含它的已缓存 CFIR 文件。
     */
    override fun getContainerCfirFile(declaration: CfirDeclaration): CfirFile? {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.containerFile(declaration)?.let { return it }
        val cjFile = declaration.psi?.containingFile as? CjFile ?: return null
        return getCachedCfirFile(cjFile)
    }

    @LLStatisticsOnlyApi
    /**
     * 返回当前语义解析要推进的全部 CFIR 文件。
     */
    override fun getAllCachedCfirFilesForResolution(): Collection<CfirFile> =
        moduleComponents.macroSemanticSnapshotStore.snapshot?.files ?: cjFileToCfirFile.values
}
