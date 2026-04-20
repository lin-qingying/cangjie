package org.cangnova.cangjie.analysis.api.components

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningCommand
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

interface CaReferenceShortener : CaLifetimeOwner {
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
