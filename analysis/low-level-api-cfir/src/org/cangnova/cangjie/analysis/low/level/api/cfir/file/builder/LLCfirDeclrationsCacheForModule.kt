/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import com.google.common.collect.MapMaker
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsOnlyApi
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
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
     * 返回指定 PSI 文件已经构建过的 raw CFIR 文件。
     */
    abstract fun getCachedCfirFile(cjFile: CjFile): CfirFile?

    /**
     * 丢弃按当前源码语义版本构建的 raw CFIR 文件。
     */
    abstract fun invalidateRawCfirFiles()

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
    private val cjFileToSource: ConcurrentMap<CjFile, LLCfirFileSourceGeneration> = MapMaker().weakKeys().makeMap()

    /**
     * 返回物理 PSI 对应的 raw CFIR 文件，缺失时原子创建并缓存。
     *
     * 宏语义快照属于 provider 的 expanded 视图，不能替换这份按 PSI 身份映射的源树。
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
        val psiFile = element.containingFile as? CjFile ?: return@synchronized null
        val snapshot = moduleComponents.macroSemanticSnapshotStore.snapshot
        if (snapshot != null && (snapshot.fileForSource(psiFile) != null || snapshot.ownsReparsedPsi(element))) {
            return@synchronized snapshot.compilationElement(element)
        }
        cjFileToSource[psiFile]?.compilationElement(element, moduleComponents.session)
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
        val snapshot = moduleComponents.macroSemanticSnapshotStore.snapshot
        if (snapshot?.fileForSource(file) != null) {
            return@synchronized mapPsiSource(syntax, snapshot::originalElement)
        }
        cjFileToSource[file]?.originalSource(source, moduleComponents.session)
    }

    /**
     * 从弱 key cache 中查询已构建 raw CFIR 文件。
     */
    override fun getCachedCfirFile(cjFile: CjFile): CfirFile? = synchronized(moduleComponents.session) {
        cjFileToSource[cjFile]?.also { it.requireConsistentInput(moduleComponents.session) }?.cfirFile
    }

    /**
     * 快照发布改变了 raw CFIR 后续解析所依赖的符号视图，旧树的已解析引用必须整体退役。
     */
    override fun invalidateRawCfirFiles() = synchronized(moduleComponents.session) {
        cjFileToSource.clear()
    }

    /**
     * 通过声明 PSI 所在文件定位包含它的已缓存 CFIR 文件。
     */
    override fun getContainerCfirFile(declaration: CfirDeclaration): CfirFile? {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.let { snapshot ->
            return snapshot.containerFile(declaration)
        }
        val cjFile = declaration.psi?.containingFile as? CjFile ?: return null
        val cfirFile = getCachedCfirFile(cjFile) ?: return null
        return cfirFile.takeIf { it.containsDeclarationIdentity(declaration) }
    }

    /**
     * 同一 PSI 可同时作为 live raw 与未发布 construction 的 source。
     * container 查询必须确认声明实际位于当前树中，不能由相同 PSI 文件替另一棵树授予 source 身份。
     * 读取当前树而非缓存成员索引，使正式 body reanalysis 后的节点替换继续遵守同一所有权。
     */
    private fun CfirFile.containsDeclarationIdentity(declaration: CfirDeclaration): Boolean {
        var found = false
        accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (found) return
                if (element === declaration) {
                    found = true
                } else {
                    element.acceptChildren(this)
                }
            }
        })
        return found
    }

    @LLStatisticsOnlyApi
    /**
     * 返回当前语义解析要推进的全部 CFIR 文件。
     */
    override fun getAllCachedCfirFilesForResolution(): Collection<CfirFile> = synchronized(moduleComponents.session) {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.files ?: cjFileToSource.values.distinct().map { source ->
            source.requireConsistentInput(moduleComponents.session)
            source.cfirFile
        }
    }
}
