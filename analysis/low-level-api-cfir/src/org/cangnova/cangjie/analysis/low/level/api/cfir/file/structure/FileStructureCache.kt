/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.util.containers.ContainerUtil
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.cfir.psi
import java.util.concurrent.ConcurrentMap

/**
 * Caches [FileStructure] instances for an [LLResolutionFacade][org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade].
 */
internal class FileStructureCache(private val moduleResolveComponents: LLCfirModuleResolveComponents) {
    /**
     * File structure elements can be rebuilt at any time and do not need to be unique (like CFIR symbols), so they can be soft-referenced
     * from this cache to reduce memory consumption. Any `analyze` call in a file causes file structure elements to be built, so during
     * operations which cause a lot of files to be analyzed (such as Find Usages), a session might accumulate a lot of file structure
     * elements.
     */
    private val cache: ConcurrentMap<CjFile, FileStructure> = ContainerUtil.createConcurrentSoftKeySoftValueMap()

    /**
     * 返回指定 PSI 文件的 file structure，缺失时构建并缓存。
     */
    fun getFileStructure(cjFile: CjFile): FileStructure {
        val cfirFile = moduleResolveComponents.cfirFileBuilder.buildRawCfirFileWithCaching(cjFile)
        val targetFile = checkNotNull(cfirFile.psi as? CjFile)
        return cache.computeIfAbsent(targetFile) { FileStructure.build(targetFile, moduleResolveComponents) }
    }

    /**
     * 返回已缓存的 file structure，不触发构建。
     */
    fun getCachedFileStructure(cjFile: CjFile): FileStructure? {
        val cfirFile = moduleResolveComponents.cache.getCachedCfirFile(cjFile) ?: return null
        return cache[cfirFile.psi as CjFile]
    }

    /** target 整树换代时，声明、表达式、partial mapper 和诊断均属于旧结构产品，整体淘汰。 */
    fun invalidateFile(cjFile: CjFile) {
        cache.remove(cjFile)
    }
}
