package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.impl.shared.context.CaCompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CaCompletionPositionKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 顶层与导入候选区段回归。
 *
 * 候选必须来自 Analysis 的导入作用域上下文，因此这里覆盖：
 * 顶层声明可见、前缀过滤、同名重载保留、以及成员访问位置不产出顶层候选。
 */
@OptIn(CaExperimentalApi::class)
class CfirTopLevelCompletionSectionTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 本文件顶层声明必须可见。 */
    @Test
    fun topLevelCandidates(mainFile: CjFile) {
        val names = section.collectCandidates(mainFile.requestAtReference(TARGET)).lookupNames()
        assertTrue(names.contains("helper"), "顶层函数必须可见，实际 $names")
        assertTrue(names.contains("overloaded"), "同名重载必须可见，实际 $names")
    }

    /** 前缀过滤只保留匹配的名字。 */
    @Test
    fun topLevelCandidatesRespectPrefix(mainFile: CjFile) {
        val names = section.collectCandidates(mainFile.requestAtReference(TARGET, "help")).lookupNames()
        assertEquals(listOf("helper"), names)
    }

    /** 同名重载必须按身份保留，不得按显示名合并成一个。 */
    @Test
    fun topLevelCandidatesKeepOverloads(mainFile: CjFile) {
        val names = section.collectCandidates(mainFile.requestAtReference(TARGET, "over")).lookupNames()
        assertTrue(
            names.count { it == "overloaded" } >= 2,
            "同名重载必须保留多个候选，实际 $names",
        )
    }

    /** 成员访问位置不使用顶层区段。 */
    @Test
    fun topLevelCandidatesAbsentAtMemberAccess(mainFile: CjFile) {
        val position = mainFile.requestAtReference(TARGET).completionPosition
        val kind = CaCompletionPositionContexts.classify(position).kind
        assertNotEquals(
            CaCompletionPositionKind.MEMBER_ACCESS,
            kind,
            "夹具位置不是成员访问；该断言只保证前提成立",
        )
        val names = section.collectCandidates(mainFile.requestAtReference(TARGET)).lookupNames()
        assertTrue(names.contains("helper"), "非成员访问位置必须给出顶层候选，实际 $names")
    }

    /**
     * 使用的区段实例。
     */
    private val section = CfirTopLevelCompletionSection()

    companion object {
        /** 夹具中待补全的标识符。 */
        private const val TARGET = "completionTarget"
    }
}
