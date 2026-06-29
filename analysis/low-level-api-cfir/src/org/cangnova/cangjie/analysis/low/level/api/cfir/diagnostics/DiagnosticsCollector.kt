/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.ModuleFileCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructureCache
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

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
     * 取得指定 PSI 元素上的 diagnostics。
     */
    fun getDiagnosticsFor(element: CjElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(element.containingCjFile)
        val structureElement = fileStructure.getStructureElementFor(element)
        val diagnostics = structureElement.diagnostics
        return diagnostics.diagnosticsFor(filter, element)
    }

    /**
     * 收集整个 PSI 文件中所有 structure element 的 diagnostics。
     */
    fun collectDiagnosticsForFile(cjFile: CjFile, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(cjFile)
        return fileStructure.getAllDiagnosticsForFile(filter)
    }
}
