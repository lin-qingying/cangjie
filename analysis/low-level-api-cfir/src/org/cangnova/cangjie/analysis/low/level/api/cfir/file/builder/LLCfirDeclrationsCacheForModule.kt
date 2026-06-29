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
     * 返回指定 PSI 文件已经构建过的 CFIR 文件。
     */
    abstract fun getCachedCfirFile(cjFile: CjFile): CfirFile?

    /**
     * 返回当前 low-level session 已构建的 CFIR 文件。
     *
     * LL CFIR 没有主编译器 `CfirProviderImpl.getAllFiles()` 那样的全量文件入口；
     * 已构建文件缓存就是 lazy resolve 阶段能安全推进的文件集合。
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
     * PSI 文件到 CFIR 文件的弱 key 缓存。
     */
    private val cjFileToCfirFile: ConcurrentMap<CjFile, CfirFile> = MapMaker().weakKeys().makeMap()

    /**
     * 返回缓存中的 CFIR 文件，缺失时原子创建并缓存。
     */
    override fun fileCached(file: CjFile, createValue: () -> CfirFile): CfirFile =
        cjFileToCfirFile.computeIfAbsent(file) { createValue() }

    /**
     * 从弱 key cache 中查询已构建 CFIR 文件。
     */
    override fun getCachedCfirFile(cjFile: CjFile): CfirFile? = cjFileToCfirFile[cjFile]

    /**
     * 通过声明 PSI 所在文件定位包含它的已缓存 CFIR 文件。
     */
    override fun getContainerCfirFile(declaration: CfirDeclaration): CfirFile? {
        val cjFile = declaration.psi?.containingFile as? CjFile ?: return null
        return getCachedCfirFile(cjFile)
    }

    @LLStatisticsOnlyApi
    /**
     * 返回当前缓存中的全部 CFIR 文件。
     */
    override fun getAllCachedCfirFilesForResolution(): Collection<CfirFile> = cjFileToCfirFile.values
}
