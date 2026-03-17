package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.source.CjLightSourceElement
import org.cangnova.cangjie.cfir.source.CjPsiSourceElement

class SourceElementPositioningStrategy(
    private val lightTreeStrategy: LightTreePositioningStrategy,
    private val psiStrategy: PositioningStrategy<*>,
    private val offsetsOnlyPositioningStrategy: OffsetsOnlyPositioningStrategy = OffsetsOnlyPositioningStrategy(),
) : AbstractSourceElementPositioningStrategy() {
    override fun markDiagnostic(diagnostic: CjDiagnosticWithSource) = when (val element = diagnostic.element) {
        is CjPsiSourceElement -> psiStrategy.markDiagnostic(diagnostic)
        is CjLightSourceElement -> lightTreeStrategy.markCjDiagnostic(element, diagnostic)
        else -> offsetsOnlyPositioningStrategy.markCjDiagnostic(element, diagnostic)
    }

    override fun isValid(element: AbstractCjSourceElement): Boolean = when (element) {
        is CjPsiSourceElement -> psiStrategy.hackyIsValid(element.psi)
        is CjLightSourceElement -> lightTreeStrategy.isValid(element.lighterASTNode, element.treeStructure)
        else -> true
    }

    private fun PositioningStrategy<*>.hackyIsValid(psi: PsiElement): Boolean {
        @Suppress("UNCHECKED_CAST")
        return (this as PositioningStrategy<PsiElement>).isValid(psi)
    }
}
