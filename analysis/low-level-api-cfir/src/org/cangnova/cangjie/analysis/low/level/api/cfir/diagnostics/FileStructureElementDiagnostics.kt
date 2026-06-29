/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic

/**
 * 单个文件结构元素按 checker filter 懒加载并组合 diagnostics 的包装。
 */
internal class FileStructureElementDiagnostics(
    /**
     * 实际执行 diagnostics 重新收集的 retriever。
     */
    private val retriever: FileStructureElementDiagnosticRetriever,
) {
    /**
     * 默认 checker 产生的 diagnostics。
     */
    private val diagnosticByDefaultCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
    }

    /**
     * extra checker 产生的 diagnostics。
     */
    private val diagnosticByExtraCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS)
    }

    /**
     * experimental checker 产生的 diagnostics。
     */
    private val diagnosticByExperimentalCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS)
    }

    /**
     * 按 filter 聚合指定 PSI 元素上的 diagnostics。
     */
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


    /**
     * 按 filter 遍历所有已收集 diagnostics 列表。
     */
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
