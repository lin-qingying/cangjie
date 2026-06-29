package org.cangnova.cangjie.analysis.api.impl.base.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.components.CaAnalysisScopeProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion

/**
 * Analysis scope provider 的 impl-base 扩展接口。
 */
@CaImplementationDetail
interface CaBaseAnalysisScopeProviderEx : CaAnalysisScopeProvider {
    /**
     * The implementation of [canBeAnalysed] without [withValidityAssertion] check.
     *
     * @see canBeAnalysed
     */
    fun canBeAnalysedImpl(element: PsiElement): Boolean

    /**
     * 在 lifetime 校验后判断当前 PSI 元素是否可被本 session 分析。
     */
    override fun PsiElement.canBeAnalysed(): Boolean = withValidityAssertion {
        canBeAnalysedImpl(this)
    }
}
