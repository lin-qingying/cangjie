/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api

/**
 * 控制 low-level diagnostics 收集时启用哪几类 checker。
 */
data class DiagnosticCheckerFilter(
    /**
     * 是否运行默认 checker 集合。
     */
    val runDefaultCheckers: Boolean,
    /**
     * 是否运行额外 checker 集合。
     */
    val runExtraCheckers: Boolean,
    /**
     * 是否运行实验性 checker 集合。
     */
    val runExperimentalCheckers: Boolean,
) {
    companion object {
        val ONLY_DEFAULT_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = true, runExtraCheckers = false, runExperimentalCheckers = false,
        )
        val ONLY_EXTRA_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = false, runExtraCheckers = true, runExperimentalCheckers = false,
        )
        val ONLY_EXPERIMENTAL_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = false, runExtraCheckers = false, runExperimentalCheckers = true,
        )
    }
}

/**
 * 合并两个 diagnostics checker 过滤器，任一侧启用的 checker 类别都会保留。
 */
operator fun DiagnosticCheckerFilter.plus(other: DiagnosticCheckerFilter) =
    DiagnosticCheckerFilter(
        runDefaultCheckers || other.runDefaultCheckers,
        runExtraCheckers || other.runExtraCheckers,
        runExperimentalCheckers || other.runExperimentalCheckers,
    )
