package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.MultiRangeReference
import org.cangnova.cangjie.idea.references.CangJieSimpleReference
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCollectionLiteralExpression
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 集合字面量本身也是一个独立语义位点。
 */
internal class CaCollectionLiteralReference(
    expression: CjCollectionLiteralExpression,
) : CangJieSimpleReference<CjCollectionLiteralExpression>(expression), MultiRangeReference {
    override val resolvesByNames: Collection<Name>
        get() = emptyList()

    override fun getRangeInElement(): TextRange = element.textRange.shiftRight(-element.textOffset)

    override fun getRanges(): List<TextRange> =
        listOfNotNull(element.leftBracket, element.rightBracket)
            .map { bracket -> bracket.textRange.shiftRight(-element.textOffset) }

    override fun resolveTargetElements() = element.resolveCallTargetPsis()

    class Provider : CangJiePsiReferenceProviderContributor<CjCollectionLiteralExpression> {
        override val elementClass: Class<CjCollectionLiteralExpression>
            get() = CjCollectionLiteralExpression::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjCollectionLiteralExpression>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { expression ->
                listOf(CaCollectionLiteralReference(expression))
            }
    }
}
