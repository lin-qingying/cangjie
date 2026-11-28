/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import com.google.common.collect.MapMaker
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsOnlyApi
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.parsing.compilationParsingResultOrNull
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.psi
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
    abstract fun fileCached(file: CjFile, createValue: () -> LLCfirFileSourceGeneration): CfirFile

    /** 原入口查询在任何声明/表达式查找前进入本代 target；退休 target 不能成为另一个原入口。 */
    abstract fun compilationElement(element: PsiElement): PsiElement?

    /** 只供修改服务读取已登记 owner，不验证或使用已经失效的旧产品对应表。 */
    abstract fun getFileSourceForModification(originFile: CjFile): LLCfirFileSourceGeneration?

    /** 同一 session 写事务中提交已经准备好的整代来源，不保留旧 target 的反查入口。 */
    abstract fun replaceFileSource(previous: LLCfirFileSourceGeneration, next: LLCfirFileSourceGeneration)

    /** 当前 target 的诊断源映射回原始编辑入口。 */
    abstract fun originalSource(source: AbstractCjSourceElement): AbstractCjSourceElement?

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
    private val cjFileToSource: ConcurrentMap<CjFile, LLCfirFileSourceGeneration> = MapMaker().weakKeys().makeMap()

    /**
     * 返回缓存中的 CFIR 文件，缺失时原子创建并缓存。
     */
    override fun fileCached(file: CjFile, createValue: () -> LLCfirFileSourceGeneration): CfirFile = synchronized(moduleComponents.session) {
        val suppliedProduct = file.compilationParsingResultOrNull()
        val origin = suppliedProduct?.originFile ?: file
        val entry = cjFileToSource[origin] ?: createValue().also {
            check(it.product.originFile === origin)
            it.requireConsistentInput(moduleComponents.session)
            it.registerDeclarationOwners()
            cjFileToSource[origin] = it
            cjFileToSource[it.product.file] = it
        }
        check(suppliedProduct == null || suppliedProduct === entry.product) { "A retired target PSI product cannot re-enter the file cache." }
        entry.requireConsistentInput(moduleComponents.session)
        entry.cfirFile
    }

    override fun compilationElement(element: PsiElement): PsiElement? = synchronized(moduleComponents.session) {
        val entry = cjFileToSource[element.containingFile as? CjFile] ?: return@synchronized null
        entry.compilationElement(element, moduleComponents.session)
    }

    override fun getFileSourceForModification(originFile: CjFile): LLCfirFileSourceGeneration? = synchronized(moduleComponents.session) {
        cjFileToSource[originFile]?.also {
            check(it.product.originFile === originFile)
            it.requireConsistentConfiguration(moduleComponents.session)
        }
    }

    override fun replaceFileSource(previous: LLCfirFileSourceGeneration, next: LLCfirFileSourceGeneration): Unit =
        synchronized(moduleComponents.session) {
            check(previous.cfirFile === next.cfirFile && previous.product.originFile === next.product.originFile)
            check(cjFileToSource[previous.product.originFile] === previous && cjFileToSource[previous.product.file] === previous)
            next.requireConsistentInput(moduleComponents.session)
            cjFileToSource.remove(previous.product.file)
            cjFileToSource[next.product.originFile] = next
            cjFileToSource[next.product.file] = next
        }

    override fun originalSource(source: AbstractCjSourceElement): AbstractCjSourceElement? = synchronized(moduleComponents.session) {
        val syntax = when (source) {
            is org.cangnova.cangjie.source.CjSourceElement -> source
            is org.cangnova.cangjie.source.CjOffsetsOnlySourceElement -> source.sourceAnchor ?: return@synchronized null
        }
        val file = syntax.psi?.containingFile as? CjFile ?: return@synchronized null
        cjFileToSource[file]?.originalSource(source, moduleComponents.session)
    }

    /**
     * 从弱 key cache 中查询已构建 CFIR 文件。
     */
    override fun getCachedCfirFile(cjFile: CjFile): CfirFile? = synchronized(moduleComponents.session) {
        cjFileToSource[cjFile]?.also { it.requireConsistentInput(moduleComponents.session) }?.cfirFile
    }

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
    override fun getAllCachedCfirFilesForResolution(): Collection<CfirFile> = synchronized(moduleComponents.session) {
        cjFileToSource.values.distinct().map { it.requireConsistentInput(moduleComponents.session); it.cfirFile }
    }
}
