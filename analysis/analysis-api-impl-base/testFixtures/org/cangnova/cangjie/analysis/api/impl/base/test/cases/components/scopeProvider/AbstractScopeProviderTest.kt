package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * `scopeProvider` 抽象测试基类。
 *
 * 这一层统一承载所有作用域类测试的断言规则，避免不同测试目录各自维护
 * `availableNames / classifier / callable` 的检查逻辑，确保 generated tests
 * 无论展开到哪个 backend 或 configurator，都共享同一套公开语义断言。
 */
abstract class AbstractScopeProviderTest : AbstractAnalysisApiComponentTest() {
    protected fun assertScopeContents(
        scope: CaScope,
        expectedAvailableNames: List<String>,
        expectedClassifiers: List<String>,
        expectedCallables: List<String>,
        scopeLabel: String,
    ) {
        val actualAvailableNames = scope.availableNames.map(Name::asString).sorted()

        expectedAvailableNames.forEach { expectedName ->
            assertTrue(
                scope.availableNames.contains(Name.identifier(expectedName)),
                "$scopeLabel 应暴露名字 `$expectedName`。实际可见名字：$actualAvailableNames",
            )
        }

        expectedClassifiers.forEach { classifierName ->
            val classifierSymbols = scope.getClassifierSymbols(Name.identifier(classifierName))
            assertEquals(
                1,
                classifierSymbols.size,
                "$scopeLabel 应能按名查询到 classifier `$classifierName`。实际可见名字：$actualAvailableNames",
            )
        }

        expectedCallables.forEach { callableName ->
            val callableSymbols = scope.getCallableSymbols(Name.identifier(callableName))
            assertEquals(
                1,
                callableSymbols.size,
                "$scopeLabel 应能按名查询到 callable `$callableName`。实际可见名字：$actualAvailableNames",
            )
        }
    }
}
