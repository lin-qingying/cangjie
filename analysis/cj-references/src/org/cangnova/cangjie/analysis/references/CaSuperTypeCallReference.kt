package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.idea.references.AbstractCjReference
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjSuperTypeCallEntry
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 超类型调用入口在 PSI 上独立存在，但当前底层语义已将其归一化为“超类型引用”。
 *
 * 因此这里直接复用 constructor callee 上的类型解析结果，
 * 把 `Base(1)` 中的 `Base` 暴露为一个 parent-level 导航入口。
 */
internal class CaSuperTypeCallReference(
    element: CjSuperTypeCallEntry,
) : AbstractCjReference<CjSuperTypeCallEntry>(element) {
    override val resolvesByNames: Collection<Name>
        get() = listOfNotNull(element.calleeExpression.constructorReferenceExpression?.referencedNameAsName)

    override fun getRangeInElement(): TextRange {
        val constructorReference = element.calleeExpression.constructorReferenceExpression ?: return TextRange.EMPTY_RANGE
        return constructorReference.referencedNameElement.textRange.shiftRight(-element.textOffset)
    }

    override fun resolveTargetElements(): Collection<PsiElement> {
        val constructorReference = element.calleeExpression.constructorReferenceExpression ?: return emptyList()
        return analyze(constructorReference) {
            constructorReference.resolveToSymbols()
                .asSequence()
                .mapNotNull { symbol -> symbol.getOriginalPsi() }
                .toList()
        }
    }

    class Provider : CangJiePsiReferenceProviderContributor<CjSuperTypeCallEntry> {
        override val elementClass: Class<CjSuperTypeCallEntry>
            get() = CjSuperTypeCallEntry::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjSuperTypeCallEntry>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { entry ->
                if (entry.calleeExpression.constructorReferenceExpression != null) {
                    listOf(CaSuperTypeCallReference(entry))
                } else {
                    emptyList()
                }
            }
    }
}
