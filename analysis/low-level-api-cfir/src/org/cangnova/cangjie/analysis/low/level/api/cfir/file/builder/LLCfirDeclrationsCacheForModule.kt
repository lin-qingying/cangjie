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
    abstract val moduleComponents: LLCfirModuleResolveComponents

    /**
     * @return [CfirFile] by [file] if it was previously built or runs [createValue] otherwise
     * The [createValue] is run under the lock so [createValue] is executed at most once for each [CjFile]
     */
    abstract fun fileCached(file: CjFile, createValue: () -> CfirFile): CfirFile

    abstract fun getContainerCfirFile(declaration: CfirDeclaration): CfirFile?

    abstract fun getCachedCfirFile(cjFile: CjFile): CfirFile?

    /**
     * 返回当前 low-level session 已构建的 CFIR 文件。
     *
     * LL CFIR 没有主编译器 `CfirProviderImpl.getAllFiles()` 那样的全量文件入口；
     * 已构建文件缓存就是 lazy resolve 阶段能安全推进的文件集合。
     */
    abstract fun getAllCachedCfirFilesForResolution(): Collection<CfirFile>

    @LLStatisticsOnlyApi
    fun getAllCachedCfirFiles(): Collection<CfirFile> = getAllCachedCfirFilesForResolution()
}

internal class ModuleFileCacheImpl(override val moduleComponents: LLCfirModuleResolveComponents) : ModuleFileCache() {
    private val cjFileToCfirFile: ConcurrentMap<CjFile, CfirFile> = MapMaker().weakKeys().makeMap()
    override fun fileCached(file: CjFile, createValue: () -> CfirFile): CfirFile =
        cjFileToCfirFile.computeIfAbsent(file) { createValue() }

    override fun getCachedCfirFile(cjFile: CjFile): CfirFile? = cjFileToCfirFile[cjFile]

    override fun getContainerCfirFile(declaration: CfirDeclaration): CfirFile? {
        val cjFile = declaration.psi?.containingFile as? CjFile ?: return null
        return getCachedCfirFile(cjFile)
    }

    @LLStatisticsOnlyApi
    override fun getAllCachedCfirFilesForResolution(): Collection<CfirFile> = cjFileToCfirFile.values
}
