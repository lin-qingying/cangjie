package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaAnalysisScopeProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * 对齐 Kotlin 的 analysis-scope provider 落位，
 * 只负责把模块内容作用域暴露给 Analysis API。
 */
internal class CaCfirAnalysisScopeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaAnalysisScopeProvider {
    override fun CaModule.analysisScope(): GlobalSearchScope = withValidityAssertion {
        contentScope
    }
}
