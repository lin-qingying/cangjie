package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.idea.references.CjSimpleReference
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.CjArrayAccessExpression
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 数组访问是独立的语义引用位点。
 *
 * 这个 reference 挂在整个 `array-access` PSI 上，只把命中范围限制到方括号 token，
 * 让 usages / target extraction 能把 `[]` 识别为对 `get` / `set` 的语义访问。
 */
internal class CaArrayAccessReference(
    expression: CjArrayAccessExpression,
) : CjSimpleReference<CjArrayAccessExpression>(expression), MultiRangeReference {
    override val resolvesByNames: Collection<Name>
        get() = listOf(OperatorNameConventions.GET, OperatorNameConventions.SET)

    override fun getRangeInElement(): TextRange = element.textRange.shiftRight(-element.textOffset)

    override fun getRanges(): List<TextRange> =
        element.bracketRanges.map { range -> range.shiftRight(-element.textOffset) }

    override fun resolveTargetElements(): Collection<PsiElement> {
        val callTargets = element.resolveCallTargetPsis { call ->
            val calleeName = call.calleeName
            calleeName == OperatorNameConventions.GET || calleeName == OperatorNameConventions.SET
        }
        if (callTargets.isNotEmpty()) return callTargets

        return analyze(element) {
            val receiverType = element.arrayExpression?.expressionType ?: return@analyze emptyList()
            val memberScope = receiverType.scope ?: return@analyze emptyList()
            memberScope.operatorCallableTargets().mapNotNull { symbol ->
                (symbol as? CaDeclarationSymbol)?.psi ?: symbol.getOriginalPsi()
            }
        }
    }

    class Provider : CangJiePsiReferenceProviderContributor<CjArrayAccessExpression> {
        override val elementClass: Class<CjArrayAccessExpression>
            get() = CjArrayAccessExpression::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjArrayAccessExpression>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { expression ->
                listOf(CaArrayAccessReference(expression))
            }
    }
}

private fun CaScope.operatorCallableTargets() = buildList {
    addAll(getCallableSymbols(OperatorNameConventions.GET))
    addAll(getCallableSymbols(OperatorNameConventions.SET))
}.distinct()
