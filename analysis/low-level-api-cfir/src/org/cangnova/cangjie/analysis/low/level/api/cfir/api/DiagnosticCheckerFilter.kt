/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api

/**
 * low-level diagnostics 的 checker 集合。
 *
 * 每个集合对应一次独立的 checker 遍历（见 `FileStructureElementDiagnostics` 的三组懒加载
 * 结果），因此它同时是"这次遍历跑了什么"的唯一可观测维度。
 *
 * @property metricSuffix 指标名中的集合段。
 */
enum class CaDiagnosticCheckerSet(val metricSuffix: String) {
    /** 常规 checker 集合（`CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS`）。 */
    DEFAULT("default"),

    /** 额外 checker 集合。 */
    EXTRA("extra"),

    /** 实验性 checker 集合。 */
    EXPERIMENTAL("experimental"),
}

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
    /**
     * 当前 filter 唯一启用的 checker 集合。
     *
     * 一次 `retrieve` 只跑一个集合，因此组合 filter（`+`）在该维度上没有唯一取值：
     * 这种情况返回 `null`，调用方按"跨集合"处理而不是硬塞进某个集合。
     */
    val checkerSet: CaDiagnosticCheckerSet?
        get() = when {
            runDefaultCheckers && !runExtraCheckers && !runExperimentalCheckers -> CaDiagnosticCheckerSet.DEFAULT
            !runDefaultCheckers && runExtraCheckers && !runExperimentalCheckers -> CaDiagnosticCheckerSet.EXTRA
            !runDefaultCheckers && !runExtraCheckers && runExperimentalCheckers -> CaDiagnosticCheckerSet.EXPERIMENTAL
            else -> null
        }

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
