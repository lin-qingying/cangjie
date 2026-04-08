package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.idea.references.AbstractCangJieReference
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjRangeExpression
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 区间表达式的 parent-level reference。
 *
 * 当前仓颉 CFIR 将 `a..b` / `a..=b` 规约为专用 `RangeExpression`，
 * 并不会像普通 operator call 那样保留可恢复的 callable 目标。
 * 因此这里绑定的是当前公开语义下最稳定的目标：结果区间类型 `Range` 的 classifier。
 */
internal class CaRangeReference(
    element: CjRangeExpression,
) : AbstractCangJieReference<CjRangeExpression>(element) {
    override val resolvesByNames: Collection<Name>
        get() = listOf(Name.identifier("Range"))

    override fun getRangeInElement(): TextRange {
        return element.operationReference.referencedNameElement.textRange.shiftRight(-element.textOffset)
    }

    override fun resolveTargetElements(): Collection<PsiElement> {
        val operatorTargets = element.resolveCallTargetPsis()
        if (operatorTargets.isNotEmpty()) return operatorTargets

        return analyze(element) {
            val classLikeSymbol = element.expressionType?.classLikeSymbol ?: return@analyze emptyList()
            listOfNotNull(classLikeSymbol.getOriginalPsi())
        }.ifEmpty {
            val containingFile = element.containingFile as? CjFile ?: return@ifEmpty emptyList()
            analyze(containingFile) {
                val rangeClassId = ClassId(StandardNames.FqNames.core, StandardNames.RANGE)
                getClassLikeSymbol(rangeClassId)
                    ?.getOriginalPsi()
                    ?.let(::listOf)
                    ?: containingFile.getFileScope()
                        .getClassifierSymbols(Name.identifier("Range"))
                        .mapNotNull { symbol -> symbol.getOriginalPsi() }
            }
        }
    }

    class Provider : CangJiePsiReferenceProviderContributor<CjRangeExpression> {
        override val elementClass: Class<CjRangeExpression>
            get() = CjRangeExpression::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjRangeExpression>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { expression ->
                listOf(CaRangeReference(expression))
            }
    }
}
