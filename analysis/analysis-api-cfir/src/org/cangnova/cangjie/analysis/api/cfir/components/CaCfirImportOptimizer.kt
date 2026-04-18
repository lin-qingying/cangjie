package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaImportOptimizer
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.psi.CjFile

/**
 * 导入优化规划入口。
 */
internal class CaCfirImportOptimizer(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaImportOptimizer {
    override fun CjFile.collectImportOptimizationPlan(): CaImportOptimizationPlan = withValidityAssertion {
        analysisSession.collectImportOptimizationPlan(this@collectImportOptimizationPlan)
    }
}
