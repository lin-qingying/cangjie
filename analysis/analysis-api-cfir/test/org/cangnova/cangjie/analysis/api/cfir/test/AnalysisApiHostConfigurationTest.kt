package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.CaTargetPlatform
import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.analysis.test.services.CaTestIdeAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.test.services.environmentManager
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * 验证 IDE 测试宿主的权限装配。
 *
 * 这组测试锁定两件事：
 * 1. IDE 模式下会注册测试专用权限检查器，而不是直接复用生产态默认策略；
 * 2. 显式禁止分析的约束仍然有效，不会因为测试宿主放宽入口而失效。
 */
class AnalysisApiHostConfigurationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/hostConfiguration",
) {
    override val configurator: AnalysisApiTestConfigurator =
        CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                frontend = FrontendKind.Cfir,
                moduleKind = TestModuleKind.Source,
                analysisSessionMode = AnalysisSessionMode.Normal,
                analysisApiMode = AnalysisApiMode.Ide,
            ),
        )

    @Test
    fun idePermissionChecker(mainFile: CjFile, testServices: TestServices) {
        val project = testServices.environmentManager.getProject()
        val permissionChecker = CaAnalysisPermissionChecker.getInstance(project)

        assertTrue(
            permissionChecker is CaTestIdeAnalysisPermissionChecker,
            "IDE 测试宿主应注册测试专用权限检查器，而不是直接复用生产态默认权限策略。",
        )

        val targetPlatform = analyzeForTest(mainFile) {
            useSiteModule.targetPlatform
        }
        assertEquals(CaTargetPlatform.IDE, targetPlatform)
    }

    @Test
    fun ideExplicitRestriction(mainFile: CjFile) {
        val permissionRegistry = CaAnalysisPermissionRegistry.getInstance()
        val restriction = CaAnalysisPermissionRegistry.CaExplicitAnalysisRestriction("test restriction")
        permissionRegistry.explicitAnalysisRestriction = restriction

        try {
            val exception = assertThrows(IllegalStateException::class.java) {
                analyzeForTest(mainFile) {
                    useSiteModule.targetPlatform
                }
            }
            assertTrue(
                exception.message?.contains("test restriction") == true,
                "显式禁止分析的原因应继续透传到测试宿主。",
            )
        } finally {
            permissionRegistry.explicitAnalysisRestriction = null
        }
    }
}
