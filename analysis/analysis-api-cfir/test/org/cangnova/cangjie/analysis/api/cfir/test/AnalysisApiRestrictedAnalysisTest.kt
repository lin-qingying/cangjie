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
    /**
     * 使用 standalone CFIR 配置运行 restricted analysis 平台服务测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 注册可切换的 restricted analysis 服务，用于精确控制平台允许和拒绝两种状态。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + RestrictedAnalysisTestServiceRegistrar

    /**
     * 验证平台禁止受限分析时，Analysis API 入口会调用服务的拒绝路径。
     */
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

    /**
     * 验证允许进入受限分析后，分析体异常会被统一包装为 `CaRestrictedAnalysisException`。
     */
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

/**
 * 用测试专用实现替换项目中的 restricted analysis 服务。
 */
private object RestrictedAnalysisTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * 在项目服务容器中安装可切换的 restricted analysis 服务。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.picoContainer.unregisterComponent(CaRestrictedAnalysisService::class.java.name)
        project.registerService(CaRestrictedAnalysisService::class.java, SwitchableCaRestrictedAnalysisService())
    }
}

/**
 * 可由测试用例切换状态的 restricted analysis 服务实现。
 */
private class SwitchableCaRestrictedAnalysisService : CaRestrictedAnalysisService {
    /**
     * 控制当前项目是否处于 restricted analysis 模式。
     */
    var enableRestrictedAnalysisMode: Boolean = true

    /**
     * 控制 restricted analysis 模式下是否允许创建分析 session。
     */
    var allowRestrictedAnalysis: Boolean = true

    /**
     * 当前是否应按受限分析模式处理 Analysis API 入口。
     */
    override val isAnalysisRestricted: Boolean
        get() = enableRestrictedAnalysisMode

    /**
     * 当前受限分析入口是否被平台策略允许。
     */
    override val isRestrictedAnalysisAllowed: Boolean
        get() = allowRestrictedAnalysis

    /**
     * 模拟平台拒绝受限分析时抛出的宿主异常。
     */
    override fun rejectRestrictedAnalysis(): Nothing {
        throw RestrictedAnalysisNotAllowedException()
    }

    /**
     * 测试专用的受限分析拒绝异常。
     */
    class RestrictedAnalysisNotAllowedException : RuntimeException("Restricted analysis is not allowed.")

    /**
     * 从项目服务中取回测试专用 restricted analysis 服务实例。
     */
    companion object {
        /**
         * 从项目服务中取回测试专用 restricted analysis 服务实例。
         */
        fun getInstance(project: Project): SwitchableCaRestrictedAnalysisService =
            CaRestrictedAnalysisService.getInstance(project) as SwitchableCaRestrictedAnalysisService
    }
}
