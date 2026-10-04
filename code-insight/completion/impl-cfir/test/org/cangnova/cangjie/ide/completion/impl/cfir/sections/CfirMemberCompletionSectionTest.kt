package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 接收者成员候选区段回归。
 *
 * 候选必须来自 Analysis 的类型作用域与 extend 提供者，因此这里覆盖：
 * 可见成员出现、private 成员被过滤、前缀过滤、extend 成员出现，
 * 以及非成员访问位置不产出成员候选。
 */
@OptIn(CaExperimentalApi::class)
class CfirMemberCompletionSectionTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 可见成员出现，private 成员不得出现。 */
    @Test
    fun memberCandidates(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(TARGET))
        val names = snapshots.lookupNames()
        assertTrue(names.contains("visible"), "可见实例成员必须出现，实际 $names")
        assertFalse(names.contains("hidden"), "private 成员不得作为候选出现，实际 $names")
        assertTrue(
            snapshots.all { it.kind == CompletionItemKind.MEMBER },
            "成员区段的候选类别必须统一为 MEMBER，实际 ${snapshots.map { it.kind }}",
        )
    }

    /** 前缀过滤只保留匹配的名字：同前缀的多个成员都要保留，未匹配的成员被排除。 */
    @Test
    fun memberCandidatesRespectPrefix(mainFile: CjFile) {
        val names = section.collectCandidates(mainFile.requestAtReference(TARGET, "v")).lookupNames()
        assertEquals(setOf("vacuum", "visible"), names.toSet())
        assertFalse(names.contains("reveal"), "不匹配前缀的成员必须被排除，实际 $names")
    }

    /** extend 提供的成员必须出现，且类别标为 EXTEND_MEMBER。 */
    @Test
    fun memberCandidatesIncludeExtend(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(TARGET))
        assertTrue(
            snapshots.lookupNames().contains("extended"),
            "extend 成员必须作为候选出现，实际 ${snapshots.lookupNames()}",
        )
        assertTrue(
            snapshots.any { it.kind == CompletionItemKind.EXTEND_MEMBER },
            "extend 成员必须标记为 EXTEND_MEMBER，实际 ${snapshots.map { it.kind }}",
        )
    }

    /** 非成员访问位置不产出成员候选。 */
    @Test
    fun memberCandidatesAbsentAtExpression(mainFile: CjFile) {
        val request = mainFile.requestAtReference(TARGET)
        val kind = CompletionPositionContexts.classify(request.completionPosition).kind
        assertEquals(CompletionPositionKind.FUNCTION_BODY, kind, "夹具位置应为函数体而非成员访问")
        assertTrue(
            section.collectCandidates(request).isEmpty(),
            "非成员访问位置不得产出成员候选",
        )
    }

    /**
     * 使用的区段实例。
     */
    private val section = CfirMemberCompletionSection()

    companion object {
        /** 夹具中待补全的标识符。 */
        private const val TARGET = "completionTarget"
    }
}
