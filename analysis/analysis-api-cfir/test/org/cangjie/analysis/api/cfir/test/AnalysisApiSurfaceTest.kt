package org.cangjie.analysis.api.cfir.test

import org.cangjie.analysis.api.cfir.test.configurators.AnalysisApiCFirSourceTestConfigurator
import org.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Analysis API 表面测试（对齐 Kotlin 的 AnalysisApiSurfaceTest）。
 *
 * 验证 Analysis API 的基本功能是否正确暴露和可用。
 */
class AnalysisApiSurfaceTest : AbstractAnalysisApiExecutionTest("analysis/analysis-api/testData/surface") {
    override val configurator: AnalysisApiTestConfigurator =
        AnalysisApiCFirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    @Disabled("TODO: 待 CFIR 模块创建逻辑就绪后启用")
    fun supertypeIteration(mainFile: CjFile) {
        // TODO: 实现超类型迭代测试
        //  analyze(implClass) {
        //      val defaultClassType = implClass.classSymbol!!.defaultType
        //      val allSupertypeSequence = defaultClassType.allSupertypes
        //      ...
        //  }
    }
}
