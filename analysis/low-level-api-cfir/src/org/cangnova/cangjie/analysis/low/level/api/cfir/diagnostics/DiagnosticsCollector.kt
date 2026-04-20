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

internal class DiagnosticsCollector(private val fileStructureCache: FileStructureCache) {
    fun getDiagnosticsFor(element: CjElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(element.containingCjFile)
        val structureElement = fileStructure.getStructureElementFor(element)
        val diagnostics = structureElement.diagnostics
        return diagnostics.diagnosticsFor(filter, element)
    }

    fun collectDiagnosticsForFile(cjFile: CjFile, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(cjFile)
        return fileStructure.getAllDiagnosticsForFile(filter)
    }
}
