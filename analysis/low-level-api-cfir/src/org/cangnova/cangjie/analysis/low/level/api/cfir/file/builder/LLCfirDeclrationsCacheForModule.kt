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
     * 返回物理 PSI 对应的 raw CFIR 文件，缺失时原子创建并缓存。
     *
     * 宏语义快照属于 provider 的 expanded 视图，不能替换这份按 PSI 身份映射的源树。
     */
    override fun fileCached(file: CjFile, createValue: () -> CfirFile): CfirFile =
        cjFileToCfirFile.computeIfAbsent(file) { createValue() }

    /**
     * 从弱 key cache 中查询已构建 raw CFIR 文件。
     */
    override fun getCachedCfirFile(cjFile: CjFile): CfirFile? = cjFileToCfirFile[cjFile]

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
