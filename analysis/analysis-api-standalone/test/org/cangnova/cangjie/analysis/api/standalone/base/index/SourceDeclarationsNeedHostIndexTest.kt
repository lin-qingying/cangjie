@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaExperimentalApi::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
)

package org.cangnova.cangjie.analysis.api.standalone.base.index

import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 按名字发现声明时，**源码声明的名字只来自宿主索引**。
 *
 * 名称发现通道由两路组成，界线是「声明有没有源码文件」：库侧（`.cjo`）由 CFIR 的只读名字视图给出，
 * 源码侧由宿主索引给出。本类锁定这条界线——宿主**没有**索引实现时：
 * 1. 源码模块里的声明不得被发现：那只能靠遍历源码文件，而代价与项目规模成正比；
 * 2. 库侧名字必须照常发现：它不依赖宿主索引，跟着一起关掉会把降级变成「不给」。
 *
 * 两路合起来才能保证「索引不在时候选变少」而不是「候选变错」或「整个通道失效」。
 */
class SourceDeclarationsNeedHostIndexTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-standalone/testData/symbolIndex",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun sourceDeclarationsNeedHostIndex(sourceDeclarationsNeedHostIndex: CjFile) {
        analyze(sourceDeclarationsNeedHostIndex) {
            val sourceNames = getDeclarationsByNamePrefix(SOURCE_TARGET).mapNotNull { it.name?.asString() }
            assertTrue(
                SOURCE_TARGET !in sourceNames,
                "宿主没有名称索引时，源码模块声明的名字不得由名字发现通道给出，实际 $sourceNames",
            )

            // 库侧（`.cjo`）名字不经过宿主索引，缺少索引不得让它一起消失。
            val libraryNames = getDeclarationsByNamePrefix("Option").mapNotNull { it.name?.asString() }
            assertTrue(
                "Option" in libraryNames,
                "库侧名字视图不依赖宿主索引，必须照常工作，实际 $libraryNames",
            )
        }
    }

    companion object {
        /** 位于依赖模块**源码**里的待发现标识符。 */
        private const val SOURCE_TARGET = "sourceOnlyLoad"
    }
}
