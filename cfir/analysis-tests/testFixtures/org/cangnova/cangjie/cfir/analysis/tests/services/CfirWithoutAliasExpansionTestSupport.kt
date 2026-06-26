package org.cangnova.cangjie.cfir.analysis.tests.services

import java.nio.charset.StandardCharsets
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
    /**
     * 当前 suppressor 依赖的指令容器。
     */
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CfirDiagnosticsDirectives)

    /**
     * suppressor 执行顺序。
     */
    override val order: Order
        get() = Order.P5

    /**
     * 根据 `SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE` 指令决定是否吞掉失败。
     *
     * 如果被 suppress 的测试已经通过，则强制失败并要求移除过期指令。
     */
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
 * Runs this test configuration only for test data containing real Cangjie type aliases.
 *
 * Aligned with Kotlin's `OnlyTestsWithTypeAliasesMetaConfigurator`.
 */
class OnlyTestsWithTypeAliasesMetaConfigurator(
    testServices: TestServices,
) : MetaTestConfigurator(testServices) {
    /**
     * 当测试数据中没有真实 type alias 声明时跳过该配置。
     */
    override fun shouldSkipTest(): Boolean {
        return testServices.moduleStructure.originalTestDataFiles.none(::containsTypeAliasDeclaration)
    }
}

/**
 * 识别仓颉 type alias 声明的正则表达式。
 */
private val TYPE_ALIAS_DECLARATION_REGEX = Regex(
    """(?m)^\s*(?:(?:public|protected|internal|private|open|abstract|override|redef|mut|unsafe|foreign|static|sealed)\s+)*type\s+[A-Za-z_][A-Za-z0-9_]*(?:\s*<[^=\r\n]*>)?\s*=""",
)

/**
 * 判断文件中是否包含仓颉 type alias 声明。
 */
private fun containsTypeAliasDeclaration(file: java.io.File): Boolean {
    val text = file.readText(StandardCharsets.UTF_8)
    return TYPE_ALIAS_DECLARATION_REGEX.containsMatchIn(text)
}
