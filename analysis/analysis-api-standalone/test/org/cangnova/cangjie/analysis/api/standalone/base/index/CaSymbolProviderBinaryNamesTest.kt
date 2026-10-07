@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaExperimentalApi::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
)

package org.cangnova.cangjie.analysis.api.standalone.base.index

import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `.cjo` 名字通道回归。
 *
 * 二进制库没有 PSI 也没有源码索引，它的顶层名字只能来自 CFIR 会话的只读名字视图。
 * 覆盖两件事：核心库声明确实能按前缀发现；发现的是**可用符号**而不是一串名字
 * （后者无法生成候选，因为渲染签名需要符号）。
 */
class CaSymbolProviderBinaryNamesTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-standalone/testData/symbolIndex",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun binaryNamesComeFromReadOnlyNameViews(binaryNamesComeFromReadOnlyNameViews: CjFile) {
        analyze(binaryNamesComeFromReadOnlyNameViews) {
            val core = getDeclarationsByNamePrefix("Int64")
            val names = core.mapNotNull { it.name?.asString() }
            assertTrue("Int64" in names, "核心库的类型必须由 .cjo 名字视图发现，实际 $names")

            // 名字只是入口：候选要渲染签名，因此必须恢复出符号，且不能把类与函数混为一谈。
            assertTrue(
                core.any { it is CaClassLikeSymbol && it.classId != null },
                "发现结果必须含带 ClassId 的类型符号，实际 ${core.map { it::class.simpleName }}",
            )
            assertTrue(
                core.any { it is CaCallableSymbol && it.callableId != null },
                "同名函数必须能以 callable 身份恢复，实际 ${core.map { it::class.simpleName }}",
            )

            val option = getDeclarationsByNamePrefix("Option").mapNotNull { it.name?.asString() }
            assertTrue("Option" in option, "标准库的 Option 必须可见，实际 $option")
        }
    }
}
