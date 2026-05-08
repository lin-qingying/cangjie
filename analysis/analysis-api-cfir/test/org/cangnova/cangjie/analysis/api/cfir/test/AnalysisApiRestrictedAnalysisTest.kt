package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisException
import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisService
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * restricted analysis 回归测试。
 *
 * 这里锁定两条平台契约：
 * 1. 当平台明确拒绝 restricted analysis 时，入口应直接被拒绝；
 * 2. 当 restricted analysis 允许进入，但分析体内部抛出运行时异常时，
 *    session provider 应将其包装为统一的受限分析异常。
 */
class AnalysisApiRestrictedAnalysisTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/restrictedAnalysis",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + RestrictedAnalysisTestServiceRegistrar

    @Test
    fun restrictedAnalysisRejected(mainFile: CjFile) {
        val restrictedAnalysisService = SwitchableCaRestrictedAnalysisService.getInstance(mainFile.project)
        restrictedAnalysisService.enableRestrictedAnalysisMode = true
        restrictedAnalysisService.allowRestrictedAnalysis = false

        assertThrows(SwitchableCaRestrictedAnalysisService.RestrictedAnalysisNotAllowedException::class.java) {
            analyzeForTest(mainFile) {
                useSiteModule.moduleDescription
            }
        }
    }

    @Test
    fun restrictedAnalysisExceptionWrapping(mainFile: CjFile) {
        val restrictedAnalysisService = SwitchableCaRestrictedAnalysisService.getInstance(mainFile.project)
        restrictedAnalysisService.enableRestrictedAnalysisMode = true
        restrictedAnalysisService.allowRestrictedAnalysis = true

        val exception = assertThrows(CaRestrictedAnalysisException::class.java) {
            analyzeForTest(mainFile) {
                error("restricted-analysis-test")
            }
        }

        assertNotNull(exception.cause, "restricted analysis 异常应保留原始 cause。")
        assertEquals("restricted-analysis-test", exception.cause!!.message)
    }
}

private object RestrictedAnalysisTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.picoContainer.unregisterComponent(CaRestrictedAnalysisService::class.java.name)
        project.registerService(CaRestrictedAnalysisService::class.java, SwitchableCaRestrictedAnalysisService())
    }
}

private class SwitchableCaRestrictedAnalysisService : CaRestrictedAnalysisService {
    var enableRestrictedAnalysisMode: Boolean = true
    var allowRestrictedAnalysis: Boolean = true

    override val isAnalysisRestricted: Boolean
        get() = enableRestrictedAnalysisMode

    override val isRestrictedAnalysisAllowed: Boolean
        get() = allowRestrictedAnalysis

    override fun rejectRestrictedAnalysis(): Nothing {
        throw RestrictedAnalysisNotAllowedException()
    }

    class RestrictedAnalysisNotAllowedException : RuntimeException("Restricted analysis is not allowed.")

    companion object {
        fun getInstance(project: Project): SwitchableCaRestrictedAnalysisService =
            CaRestrictedAnalysisService.getInstance(project) as SwitchableCaRestrictedAnalysisService
    }
}
