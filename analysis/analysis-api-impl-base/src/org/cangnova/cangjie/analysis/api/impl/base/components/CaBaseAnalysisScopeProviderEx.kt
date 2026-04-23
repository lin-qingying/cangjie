package org.cangnova.cangjie.analysis.api.impl.base.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.components.CaAnalysisScopeProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion

@CaImplementationDetail
interface CaBaseAnalysisScopeProviderEx : CaAnalysisScopeProvider {
    /**
     * The implementation of [canBeAnalysed] without [withValidityAssertion] check.
     *
     * @see canBeAnalysed
     */
    fun canBeAnalysedImpl(element: PsiElement): Boolean

    override fun PsiElement.canBeAnalysed(): Boolean = withValidityAssertion {
        canBeAnalysedImpl(this)
    }
}
