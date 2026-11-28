/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.util.containers.ContainerUtil
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.ModuleFileCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructureCache
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import java.util.concurrent.ConcurrentMap

/**
 * 基于文件结构缓存按 PSI 元素或文件收集 low-level diagnostics。
 */
internal class DiagnosticsCollector(
    /**
     * 从 PSI 文件映射到可诊断结构元素的缓存。
     */
    private val fileStructureCache: FileStructureCache,
) {
    /**
     * Platform 诊断按 use-site session 缓存；软键/软值遵循 LL structure 缓存的生命周期。
     */
    private val contextualDiagnostics: ConcurrentMap<ContextualDiagnosticsKey, FileStructureElementDiagnosticList> =
        ContainerUtil.createConcurrentSoftKeySoftValueMap()

    /**
     * 取得指定 PSI 元素上的 diagnostics。
     */
    fun getDiagnosticsFor(
        element: CjElement,
        filter: DiagnosticCheckerFilter,
        platformCheckerComponents: LLCfirModuleResolveComponents? = null,
    ): List<CjPsiDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(element.containingCjFile)
        val structureElement = fileStructure.getStructureElementFor(element)
        val diagnostics = structureElement.diagnostics
        return diagnostics.diagnosticsFor(filter, element, platformCheckerComponents)
    }

    /**
     * 收集整个 PSI 文件中所有 structure element 的 diagnostics。
     */
    fun collectDiagnosticsForFile(
        cjFile: CjFile,
        filter: DiagnosticCheckerFilter,
        platformCheckerComponents: LLCfirModuleResolveComponents? = null,
    ): List<CjPsiDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(cjFile)
        return fileStructure.getAllDiagnosticsForFile(filter, platformCheckerComponents)
    }

    /**
     * 返回指定文件结构声明在 checker kind 与 filter 下的缓存诊断；失败结果不会进入缓存。
     */
    internal fun getOrComputeContextualDiagnostics(
        diagnostics: FileStructureElementDiagnostics,
        filter: DiagnosticCheckerFilter,
        checkerKind: MppCheckerKind,
        compute: () -> FileStructureElementDiagnosticList,
    ): FileStructureElementDiagnosticList {
        val key = ContextualDiagnosticsKey(diagnostics, filter, checkerKind)
        return contextualDiagnostics.computeIfAbsent(key) { compute() }
    }

    /** 同一文件的整树 source 换代使所有跨 session 诊断投影的旧范围失效。 */
    fun invalidateSourceGeneration() {
        contextualDiagnostics.clear()
    }

    private data class ContextualDiagnosticsKey(
        val diagnostics: FileStructureElementDiagnostics,
        val filter: DiagnosticCheckerFilter,
        val checkerKind: MppCheckerKind,
    )
}
