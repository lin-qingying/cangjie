package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateStatus
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 补全候选可达性分类的**行为规则**（设计 §6.6：失败、未知与不可见不得全部压成 HIDDEN）。
 *
 * 这条规则靠两个属性表达（[CaCompletionCandidateStatus.shouldHide] /
 * [CaCompletionCandidateStatus.isDeterminedUsable]），而不是让每个消费者各自 `when` 一遍——
 * 各写一遍时，「未知」迟早会在某一条路径上被当成否定处理。
 */
class CaCompletionCandidateStatusContractTest {
    /**
     * 未知**不得**隐藏：判定不出来不等于不可用。
     */
    @Test
    fun unknownIsNotHidden() {
        assertFalse(
            CaCompletionCandidateStatus.UNKNOWN.shouldHide,
            "未知不得据此隐藏候选——把不确定当否定会让用户少候选且毫无提示",
        )
    }

    /**
     * 只有「确定不可用」的两态才移除候选。
     */
    @Test
    fun onlyDeterminedUnusableStatesAreHidden() {
        assertTrue(CaCompletionCandidateStatus.HIDDEN.shouldHide, "确定不可访问必须移除")
        assertTrue(CaCompletionCandidateStatus.NOT_APPLICABLE.shouldHide, "不适用必须移除")
        assertFalse(CaCompletionCandidateStatus.DIRECT.shouldHide, "直接可达不得移除")
        assertFalse(CaCompletionCandidateStatus.REQUIRES_IMPORT.shouldHide, "需要导入不得移除")
    }

    /**
     * 「确定可用」只含能落地的两态，未知不在其中。
     *
     * 需要写下确定性结果的动作（例如引用缩短要写一个一定能解析的短名）按本属性判——
     * 用 `!shouldHide` 会把它错误地放行。
     */
    @Test
    fun determinedUsableExcludesUnknown() {
        assertTrue(CaCompletionCandidateStatus.DIRECT.isDeterminedUsable)
        assertTrue(CaCompletionCandidateStatus.REQUIRES_IMPORT.isDeterminedUsable)
        assertFalse(CaCompletionCandidateStatus.UNKNOWN.isDeterminedUsable, "未知不能算确定可用")
        assertFalse(CaCompletionCandidateStatus.HIDDEN.isDeterminedUsable)
        assertFalse(CaCompletionCandidateStatus.NOT_APPLICABLE.isDeterminedUsable)
    }

    /**
     * 穷尽性：每个状态都必须落在分类里，且两个分类互斥。
     *
     * 这条是给未来加状态的人看的：新增一个状态时，若忘了想清它属于哪一类，
     * 本用例会红——而漏想的后果是它在某条路径上被当成「可以」或「不可以」，静默地错。
     */
    @Test
    fun everyStatusIsClassifiedAndClassificationsAreExclusive() {
        CaCompletionCandidateStatus.entries.forEach { status ->
            assertFalse(
                status.shouldHide && status.isDeterminedUsable,
                "$status 不能既该隐藏又确定可用",
            )
        }
        val classified = CaCompletionCandidateStatus.entries.filter { it.shouldHide || it.isDeterminedUsable }
        assertTrue(
            classified.map { it.name }.containsAll(listOf("DIRECT", "REQUIRES_IMPORT", "HIDDEN", "NOT_APPLICABLE")),
            "四个确定状态都必须落在分类里，实际 ${classified.map { it.name }}",
        )
        // 未知刻意**两个都不属于**：它不是「确定可用」，也不该被当成「确定不可用」。
        assertTrue(
            !CaCompletionCandidateStatus.UNKNOWN.shouldHide && !CaCompletionCandidateStatus.UNKNOWN.isDeterminedUsable,
            "未知必须两个分类都不属于",
        )
    }
}
