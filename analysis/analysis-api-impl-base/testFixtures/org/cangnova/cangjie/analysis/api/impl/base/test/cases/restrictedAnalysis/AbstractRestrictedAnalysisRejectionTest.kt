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
 */
abstract class AbstractRestrictedAnalysisRejectionTest : AbstractRestrictedAnalysisTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

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

    object Directives : SimpleDirectivesContainer() {
        val RESTRICTED by stringDirective(
            description = "是否启用 restricted analysis 模式。",
            applicability = DirectiveApplicability.Module,
        )

        val ALLOWED by stringDirective(
            description = "在 restricted analysis 模式下是否允许进入分析。",
            applicability = DirectiveApplicability.Module,
        )

        val EXPECT_REJECTION by directive(
            description = "是否期望当前分析入口被拒绝。",
            applicability = DirectiveApplicability.Module,
        )
    }
}
