/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic

internal class FileStructureElementDiagnostics(private val retriever: FileStructureElementDiagnosticRetriever) {
    private val diagnosticByDefaultCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
    }

    private val diagnosticByExtraCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS)
    }

    private val diagnosticByExperimentalCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS)
    }

    fun diagnosticsFor(filter: DiagnosticCheckerFilter, element: PsiElement): List<CjPsiDiagnostic> =
        SmartList<CjPsiDiagnostic>().apply {
            if (filter.runDefaultCheckers) {
                addAll(diagnosticByDefaultCheckers.diagnosticsFor(element))
            }
            if (filter.runExtraCheckers) {
                addAll(diagnosticByExtraCheckers.diagnosticsFor(element))
            }
            if (filter.runExperimentalCheckers) {
                addAll(diagnosticByExperimentalCheckers.diagnosticsFor(element))
            }
        }


    inline fun forEach(filter: DiagnosticCheckerFilter, action: (List<CjPsiDiagnostic>) -> Unit) {
        if (filter.runDefaultCheckers) {
            diagnosticByDefaultCheckers.forEach(action)
        }
        if (filter.runExtraCheckers) {
            diagnosticByExtraCheckers.forEach(action)
        }
        if (filter.runExperimentalCheckers) {
            diagnosticByExperimentalCheckers.forEach(action)
        }
    }
}
