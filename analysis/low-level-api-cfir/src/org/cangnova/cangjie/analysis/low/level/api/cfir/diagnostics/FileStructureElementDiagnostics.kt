/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
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
     * 按声明 owner 与当前 leaf 分别收集 Common/Platform 检查，避免把依赖声明的结果写入错误 session。
     */
    fun diagnosticsFor(
        filter: DiagnosticCheckerFilter,
        element: PsiElement,
        platformCheckerComponents: LLCfirModuleResolveComponents?,
    ): List<CjPsiDiagnostic> {
        if (platformCheckerComponents == null || platformCheckerComponents === retriever.moduleComponents) {
            return diagnosticsFor(filter, element)
        }

        return SmartList<CjPsiDiagnostic>().apply {
            diagnosticListsFor(filter, platformCheckerComponents).forEach { addAll(it.diagnosticsFor(element)) }
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

    /**
     * 按当前 collection context 返回结果集；跨模块时使用 owner Common 与 leaf Platform 两个独立 pass。
     */
    fun forEach(
        filter: DiagnosticCheckerFilter,
        platformCheckerComponents: LLCfirModuleResolveComponents?,
        action: (List<CjPsiDiagnostic>) -> Unit,
    ) {
        if (platformCheckerComponents == null || platformCheckerComponents === retriever.moduleComponents) {
            forEach(filter, action)
            return
        }

        diagnosticListsFor(filter, platformCheckerComponents).forEach { it.forEach(action) }
    }

    private fun diagnosticListsFor(
        filter: DiagnosticCheckerFilter,
        platformCheckerComponents: LLCfirModuleResolveComponents,
    ): List<FileStructureElementDiagnosticList> = buildList {
        val ownerComponents = retriever.moduleComponents
        val checkerFilters = buildList {
            if (filter.runDefaultCheckers) add(DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
            if (filter.runExtraCheckers) add(DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS)
            if (filter.runExperimentalCheckers) add(DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS)
        }

        for (checkerFilter in checkerFilters) {
            add(
                ownerComponents.diagnosticsCollector.getOrComputeContextualDiagnostics(
                    this@FileStructureElementDiagnostics,
                    checkerFilter,
                    MppCheckerKind.Common,
                ) {
                    retriever.retrieve(checkerFilter, ownerComponents, MppCheckerKind.Common)
                }
            )
            add(
                platformCheckerComponents.diagnosticsCollector.getOrComputeContextualDiagnostics(
                    this@FileStructureElementDiagnostics,
                    checkerFilter,
                    MppCheckerKind.Platform,
                ) {
                    retriever.retrieve(checkerFilter, platformCheckerComponents, MppCheckerKind.Platform)
                }
            )
        }
    }
}
