package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTokenFactory
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaReadActionConfinementLifetimeTokenFactory
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionOptions
import org.cangnova.cangjie.analysis.api.platform.permissions.CaDefaultAnalysisPermissionOptions
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.analysis.test.services.environmentManager
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * 验证 IDE 测试宿主的权限装配。
 *
 * 这组测试锁定两件事：
 * 1. IDE mode 测试宿主会装配 Kotlin 对位的默认权限选项与 read-action confinement token factory；
 * 2. 显式禁止分析的约束仍然有效，不会因为测试宿主覆盖宿主装配而失效。
 */
class AnalysisApiHostConfigurationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/hostConfiguration",
) {
    /**
     * 使用 CFIR IDE-mode factory 创建配置器，以验证 IDE 宿主服务装配路径。
     */
    override val configurator: AnalysisApiTestConfigurator =
        CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                frontend = FrontendKind.Cfir,
                moduleKind = TestModuleKind.Source,
                analysisSessionMode = AnalysisSessionMode.Normal,
                analysisApiMode = AnalysisApiMode.Ide,
            ),
        )

    /**
     * 验证 IDE-mode 测试宿主注册了权限检查、权限选项和 lifetime token factory。
     */
    @Test
    fun idePermissionChecker(mainFile: CjFile, testServices: TestServices) {
        val project = testServices.environmentManager.getProject()
        val permissionChecker = CaAnalysisPermissionChecker.getInstance(project)
        val permissionOptions = CaAnalysisPermissionOptions.getInstance()
        val lifetimeTokenFactory = CaLifetimeTokenFactory.getInstance(project)

        assertTrue(
            permissionOptions is CaDefaultAnalysisPermissionOptions,
            "IDE 测试宿主应注册默认 Analysis 权限选项。",
        )
        assertTrue(
            lifetimeTokenFactory is CaReadActionConfinementLifetimeTokenFactory,
            "IDE 测试宿主应注册 read-action confinement lifetime token factory。",
        )
        assertTrue(
            permissionChecker is CaAnalysisPermissionChecker,
            "IDE 测试宿主应保留 Analysis 权限检查器服务。",
        )

        analyzeForTest(mainFile) {
            useSiteModule.moduleDescription
        }
    }

    /**
     * 验证显式 analysis restriction 会阻止 IDE-mode 测试宿主创建分析 session。
     */
    @Test
    fun ideExplicitRestriction(mainFile: CjFile) {
        val permissionRegistry = CaAnalysisPermissionRegistry.getInstance()
        val restriction = CaAnalysisPermissionRegistry.CaExplicitAnalysisRestriction("test restriction")
        permissionRegistry.explicitAnalysisRestriction = restriction

        try {
            val exception = assertThrows(IllegalStateException::class.java) {
                analyzeForTest(mainFile) {
                    useSiteModule.moduleDescription
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
