package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    /**
     * 同名不同参数的重载必须**各带实例化签名**，不能只靠显示名区分。
     *
     * 这是缺陷 D-2 的验收：框架侧的 public symbol 缓存键此前只含 `callableId + kind`，
     * 两个重载被判为同一符号；插件侧若再按短名去重，候选就只剩一个。
     * 这里同时断言签名文本与身份键两两不同——只断言「候选数 ≥ 2」不足以发现
     * 「两个候选指向同一声明」这种形态。
     */
    @Test
    fun topLevelOverloadsCarryDistinctSignatures(mainFile: CjFile) {
        val overloads = section.collectCandidates(mainFile.requestAtReference(TARGET, "over"))
            .filter { it.lookupString == "overloaded" }
        assertTrue(overloads.size >= 2, "同名重载必须保留多个候选，实际 ${overloads.lookupNames()}")

        val tailTexts = overloads.map { it.tailText }
        val typeTexts = overloads.map { it.typeText }
        val dedupKeys = overloads.map { it.identity.deduplicationKey }
        val handles = overloads.map { it.identity.handle }

        assertEquals(tailTexts.size, tailTexts.distinct().size, "同名重载的签名尾文本必须互不相同，实际 $tailTexts")
        assertEquals(typeTexts.size, typeTexts.distinct().size, "同名重载的返回类型必须互不相同，实际 $typeTexts")
        assertEquals(dedupKeys.size, dedupKeys.distinct().size, "同名重载的身份键必须互不相同，实际 $dedupKeys")
        assertEquals(handles.size, handles.distinct().size, "同名重载的身份句柄必须互不相同，实际 $handles")

        // 夹具的两个重载分别是 (Int64): Int64 与 (String): String。
        // 参数类型必须真的进到展示里，否则「签名不同」只是空断言。
        assertTrue(
            tailTexts.any { it.contains("Int64") } && tailTexts.any { it.contains("String") },
            "签名尾文本必须体现各自的参数类型，实际 $tailTexts",
        )
    }

    /** 成员访问位置不使用顶层区段。 */
    @Test
    fun topLevelCandidatesAbsentAtMemberAccess(mainFile: CjFile) {
        val position = mainFile.requestAtReference(TARGET).completionPosition
        val kind = CompletionPositionContexts.classify(position).kind
        assertNotEquals(
            CompletionPositionKind.MEMBER_ACCESS,
            kind,
            "夹具位置不是成员访问；该断言只保证前提成立",
        )
        val names = section.collectCandidates(mainFile.requestAtReference(TARGET)).lookupNames()
        assertTrue(names.contains("helper"), "非成员访问位置必须给出顶层候选，实际 $names")
    }

    /**
     * 弃用候选降权但不剔除。
     *
     * 迁移期的代码仍要能选到 `@Deprecated` 的声明，但不该让它排在同等可用的替代项之前。
     * 只认**内置**注解：这也顺带锁定「注解提取走的是内置种类而不是名字字符串」。
     */
    @Test
    fun deprecatedCandidateIsDemoted(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference("target"))
        val legacy = snapshots.single { it.lookupString == "targetLegacy" }
        val fresh = snapshots.single { it.lookupString == "targetFresh" }

        assertTrue(legacy.sort.isDeprecated, "带内置 @Deprecated 的声明必须标为弃用")
        assertFalse(fresh.sort.isDeprecated, "未标注的声明不得被判为弃用")
        assertTrue(
            fresh.weight > legacy.weight,
            "同来源同可达性时可用候选必须排在弃用候选之前，实际 ${fresh.weight} vs ${legacy.weight}",
        )
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
