package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjFakePsiSourceElementWithCustomOffsetStrategy
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.SuspiciousFakeSourceCheck

/**
 * 同时支持 PSI、LightTree 和 offset-only 源元素的组合定位策略。
 */
@OptIn(SuspiciousFakeSourceCheck::class)
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
        // 自定义 offset 的 fake PSI 仍然携带原始 PSI 作为语义锚点，但其文本范围已经
        // 明确由 source element 覆盖。不能再让 PSI 定位策略从底层节点重新取完整范围，
        // 否则脱糖 operator、合成调用等诊断会把 synthetic source 的尾部一并标记。
        is CjFakePsiSourceElementWithCustomOffsetStrategy ->
            offsetsOnlyPositioningStrategy.markCjDiagnostic(element, diagnostic)
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
