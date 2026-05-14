package org.cangnova.cangjie.analysis.api.components

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningCommand
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * 引用缩短(import shorten)协议。
 *
 * 设计要点/职责:
 * - 在不修改源码的前提下,计算可被缩短的引用集合与需要补齐的 import,
 *   将分析结果与真正的写入动作解耦。
 * - 协议层只暴露稳定的命令/计划数据,不暴露策略类与缩短引擎细节。
 *
 * 对齐 Kotlin Analysis API 的 `KaReferenceShortener`。
 */
interface CaReferenceShortener : CaLifetimeOwner {
    /**
     * 收集该文件全部可缩短的引用方案,作为执行前的全局快照。
     */
    fun CjFile.collectReferenceShorteningPlan(): CaReferenceShorteningPlan

    /**
     * 按选择范围收集真正要执行的缩短命令。
     *
     * 该入口与 Kotlin `collectPossibleReferenceShortenings(file, selection)` 对位，
     * 但当前只暴露已经稳定的公共结果：
     * 1. 命中的操作。
     * 2. 需要补齐的 imports。
     */
    fun CjFile.collectReferenceShortenings(
        selection: TextRange = textRange,
    ): CaReferenceShorteningCommand

    /**
     * 按单个 PSI 元素范围收集缩短命令。
     */
    fun CjElement.collectReferenceShorteningsInElement(): CaReferenceShorteningCommand
}
