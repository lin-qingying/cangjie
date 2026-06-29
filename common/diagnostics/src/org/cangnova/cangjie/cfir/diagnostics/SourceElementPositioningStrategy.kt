package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement

/**
 * 同时支持 PSI、LightTree 和 offset-only 源元素的组合定位策略。
 */
class SourceElementPositioningStrategy(
    /**
     * LightTree 源元素使用的定位策略。
     */
    private val lightTreeStrategy: LightTreePositioningStrategy,
    /**
     * PSI 源元素使用的定位策略。
     */
    private val psiStrategy: PositioningStrategy<*>,
    /**
     * 其他源元素使用的 offset-only 定位策略。
     */
    private val offsetsOnlyPositioningStrategy: OffsetsOnlyPositioningStrategy = OffsetsOnlyPositioningStrategy(),
) : AbstractSourceElementPositioningStrategy() {
    /**
     * 按诊断源元素类型分派到对应定位策略。
     */
    override fun markDiagnostic(diagnostic: CjDiagnosticWithSource) = when (val element = diagnostic.element) {
        is CjPsiSourceElement -> psiStrategy.markDiagnostic(diagnostic)
        is CjLightSourceElement -> lightTreeStrategy.markCjDiagnostic(element, diagnostic)
        else -> offsetsOnlyPositioningStrategy.markCjDiagnostic(element, diagnostic)
    }

    /**
     * 按源元素类型检查对应定位策略是否有效。
     */
    override fun isValid(element: AbstractCjSourceElement): Boolean = when (element) {
        is CjPsiSourceElement -> psiStrategy.hackyIsValid(element.psi)
        is CjLightSourceElement -> lightTreeStrategy.isValid(element.lighterASTNode, element.treeStructure)
        else -> true
    }

    /**
     * 在泛型被擦除的 PSI 定位策略上执行有效性检查。
     */
    private fun PositioningStrategy<*>.hackyIsValid(psi: PsiElement): Boolean {
        @Suppress("UNCHECKED_CAST")
        return (this as PositioningStrategy<PsiElement>).isValid(psi)
    }
}
