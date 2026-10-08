package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 名称索引不可用时的降级。
 *
 * 本类**刻意不注册名称索引服务**：这正是宿主没有索引实现、
 * 索引尚未构建完成（IDE 的 dumb mode）、或索引构建失败时的形态。
 *
 * 降级的要求是「少给」而不是「乱给」，也不是「不给」：
 * 1. 源码模块里的声明**只能靠索引找到**，索引不在时它们必须缺席——
 *    否则说明有人绕开索引去扫了源码，而那一步的代价与项目规模成正比；
 * 2. 库侧（`.cjo`）名字走的是 CFIR 的库侧只读视图，不依赖宿主索引，因此必须照常给出。
 * 两条合起来才是「降级」：候选少了，但少得有边界。
 */
@OptIn(CaExperimentalApi::class)
class CfirIndexDegradationTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 索引不可用时，源码模块的声明必须缺席。
     *
     * 夹具里的 `indexedLoad` 位于依赖模块的**源码**中：它既不在导入作用域里，
     * 也不在任何作用域链上，只能靠索引发现。因此它一旦出现，就证明候选来自
     * 「遍历源码文件」这条兜底路径——那正是索引要取代的代价。
     */
    @Test
    fun indexAbsentDropsSourceModuleCandidates(indexAbsentDropsSourceModuleCandidates: CjFile) {
        val names = section.collectCandidates(
            indexAbsentDropsSourceModuleCandidates.requestAtReference(SOURCE_TARGET),
        ).lookupNames()

        assertTrue(
            SOURCE_TARGET !in names,
            "索引不可用时不得给出只能靠索引找到的源码模块候选，实际 $names",
        )
        // 库侧名字通道与索引无关，降级不得把它一起关掉：
        // 若这里为空，说明降级退化成「不给」，而不是「少给」。
        assertTrue(
            names.isNotEmpty(),
            "库侧（.cjo）名字不依赖宿主索引，降级时仍须给出候选：空结果意味着降级过度",
        )
    }

    /**
     * 使用的区段实例。
     */
    private val section = CfirIndexCompletionSection()

    companion object {
        /** 夹具中位于依赖模块**源码**里的标识符。 */
        private const val SOURCE_TARGET = "indexedLoad"
    }
}
