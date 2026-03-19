package org.cangnova.cangjie.cfir.analysis.tests.services

import org.cangnova.cangjie.test.WrappedException
import org.cangnova.cangjie.test.config.TestPhaseDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.services.MetaTestConfigurator
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.moduleStructure

/**
 * Suppressor used by `AbstractCfirLightTreeDiagnosticsWithoutAliasExpansionTest`.
 *
 * Aligned with Kotlin's `FirWithoutAliasExpansionTestSuppressor`.
 */
class CfirWithoutAliasExpansionTestSuppressor(
    testServices: TestServices,
) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestPhaseDirectives)

    override val order: Order
        get() = Order.P5

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE !in testServices.moduleStructure.allDirectives) {
            return failedAssertions
        }

        return when {
            failedAssertions.isEmpty() -> testServices.assertions.fail {
                "Test is passing. Remove ${CfirDiagnosticsDirectives.SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE} directive"
            }

            else -> emptyList()
        }
    }
}

/**
 * Runs this test configuration only for test data containing `typealias`.
 *
 * Aligned with Kotlin's `OnlyTestsWithTypeAliasesMetaConfigurator`.
 */
class OnlyTestsWithTypeAliasesMetaConfigurator(
    testServices: TestServices,
) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        return testServices.moduleStructure.originalTestDataFiles.none { file ->
            file.readText().contains("typealias")
        }
    }
}

