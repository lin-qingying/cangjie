package org.cangnova.cangjie.analysis.api.impl.base.test.cases.restrictedAnalysis

import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * restricted analysis 拒绝策略抽象测试。
 *
 * 测试通过模块级指令控制 restricted mode 与 allow 状态，并断言公开分析入口是否被拒绝。
 */
abstract class AbstractRestrictedAnalysisRejectionTest : AbstractRestrictedAnalysisTest() {
    /**
     * 当前拒绝策略测试额外注册的模块级指令。
     *
     * 指令描述受限模式、允许状态以及是否期望拒绝。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    /**
     * 执行 restricted analysis 拒绝策略断言。
     *
     * 方法先根据指令设置测试服务开关，再进入一次分析入口，比较是否抛出拒绝异常。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = mainModule.testModule.directives
        val isRestricted = directives[Directives.RESTRICTED].singleOrNull().toBoolean()
        val isAllowed = directives[Directives.ALLOWED].singleOrNull().toBoolean()
        val expectRejection = directives.contains(Directives.EXPECT_REJECTION)

        val restrictedService = mainModule.restrictedAnalysisService
        restrictedService.enableRestrictedAnalysisMode = isRestricted
        restrictedService.allowRestrictedAnalysis = isAllowed

        val rejected = try {
            analyzeForTest(mainFile) {
                useSiteModule.moduleDescription
            }
            false
        } catch (_: SwitchableCaRestrictedAnalysisService.RestrictedAnalysisNotAllowedException) {
            true
        }

        assertEquals(expectRejection, rejected)
    }

    /**
     * restricted analysis 拒绝策略测试的指令集合。
     *
     * 这些指令作用于测试模块，描述进入分析前要安装到服务中的受限状态。
     */
    object Directives : SimpleDirectivesContainer() {
        /**
         * 是否启用 restricted analysis 模式。
         *
         * 指令值会转换为布尔值并写入 `enableRestrictedAnalysisMode`。
         */
        val RESTRICTED by stringDirective(
            description = "是否启用 restricted analysis 模式。",
            applicability = DirectiveApplicability.Module,
        )

        /**
         * 在 restricted analysis 模式下是否允许进入分析。
         *
         * 指令值会转换为布尔值并写入 `allowRestrictedAnalysis`。
         */
        val ALLOWED by stringDirective(
            description = "在 restricted analysis 模式下是否允许进入分析。",
            applicability = DirectiveApplicability.Module,
        )

        /**
         * 当前用例是否期望分析入口被拒绝。
         *
         * 该指令存在即表示测试应捕获到 `RestrictedAnalysisNotAllowedException`。
         */
        val EXPECT_REJECTION by directive(
            description = "是否期望当前分析入口被拒绝。",
            applicability = DirectiveApplicability.Module,
        )
    }
}
