package org.cangnova.cangjie.analysis.api.impl.base.test.cases.restrictedAnalysis

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisService
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * restricted analysis 抽象测试基座。
 *
 * 这里统一为测试宿主替换可切换的 `CaRestrictedAnalysisService`，
 * 让具体用例专注于“拒绝策略”和“异常包装”两条公开契约本身。
 */
abstract class AbstractRestrictedAnalysisTest : AbstractAnalysisApiBasedTest() {
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + listOf(RestrictedAnalysisTestServiceRegistrar)

    protected val CjTestModule.restrictedAnalysisService: SwitchableCaRestrictedAnalysisService
        get() = SwitchableCaRestrictedAnalysisService.getInstance(caModule.project)
}

private object RestrictedAnalysisTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.picoContainer.unregisterComponent(CaRestrictedAnalysisService::class.java.name)
        project.registerService(CaRestrictedAnalysisService::class.java, SwitchableCaRestrictedAnalysisService())
    }
}

class SwitchableCaRestrictedAnalysisService : CaRestrictedAnalysisService {
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
