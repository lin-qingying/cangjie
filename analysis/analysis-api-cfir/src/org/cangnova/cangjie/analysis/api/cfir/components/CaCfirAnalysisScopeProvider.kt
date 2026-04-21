package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaAnalysisScopeProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScope

/**
 * 对齐 Kotlin 的 analysis-scope provider 落位，
 * 只负责把模块内容作用域暴露给 Analysis API。
 */
internal class CaCfirAnalysisScopeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaAnalysisScopeProvider {
    @OptIn(CaPlatformInterface::class)
    private val resolutionScope: CaResolutionScope
        get() = CaResolutionScope.forModule(analysisSession.useSiteModule)

    @OptIn(CaPlatformInterface::class)
    override val analysisScope: GlobalSearchScope
        get() = withValidityAssertion { resolutionScope }

    @OptIn(CaPlatformInterface::class)
    override fun PsiElement.canBeAnalysed(): Boolean = withValidityAssertion {
        resolutionScope.contains(this)
    }
}
