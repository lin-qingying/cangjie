/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.util.containers.ContainerUtil
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.psi.CjFile
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

    fun getFileStructure(cjFile: CjFile): FileStructure = cache.computeIfAbsent(cjFile) {
        FileStructure.build(cjFile, moduleResolveComponents)
    }

    fun getCachedFileStructure(cjFile: CjFile): FileStructure? = cache[cjFile]
}
