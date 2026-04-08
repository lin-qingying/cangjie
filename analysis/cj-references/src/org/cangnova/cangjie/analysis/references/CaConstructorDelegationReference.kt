package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.idea.references.CangJieSimpleReference
import org.cangnova.cangjie.psi.CjConstructorDelegationReferenceExpression
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * `this(...)` / `super(...)` 是独立的 constructor-delegation 引用位点。
 */
internal class CaConstructorDelegationReference(
    expression: CjConstructorDelegationReferenceExpression,
) : CangJieSimpleReference<CjConstructorDelegationReferenceExpression>(expression) {
    override fun getRangeInElement(): TextRange = TextRange(0, element.textLength)

    override fun resolveTargetElements() = element.resolveCallTargetPsis()

    class Provider : CangJiePsiReferenceProviderContributor<CjConstructorDelegationReferenceExpression> {
        override val elementClass: Class<CjConstructorDelegationReferenceExpression>
            get() = CjConstructorDelegationReferenceExpression::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjConstructorDelegationReferenceExpression>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { expression ->
                listOf(CaConstructorDelegationReference(expression))
            }
    }
}
