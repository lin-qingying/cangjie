package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaImportOptimizer
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.psi.CjFile

/**
 * 导入优化规划入口。
 */
internal class CaCfirImportOptimizer(
    /**
     * 延迟取得当前 CFIR Analysis session，实际优化规划由 session 级实现完成。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaImportOptimizer {
    /**
     * 收集当前文件的导入优化规划。
     */
    override fun CjFile.collectImportOptimizationPlan(): CaImportOptimizationPlan = withValidityAssertion {
        analysisSession.collectImportOptimizationPlan(this@collectImportOptimizationPlan)
    }
}
