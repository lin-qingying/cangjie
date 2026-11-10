package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
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

    /**
     * 未导入 receiver 的恢复要查名称索引，测试宿主需要补注册该服务。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(IndexSectionTestServiceRegistrar)

    /**
     * `super.` 之后给父类的成员。
     *
     * `super` 的接收者不是表达式而是关键字：它没有 `expressionType`，
     * 按普通接收者解析必然拿不到类型，结果是用户在 `return super.` 后面一个候选都得不到。
     * 父类取超类型列表里的**第一个**——仓颉的 super 只指向直接父类，接口不参与。
     */
    @Test
    fun superQualifierOffersParentMembers(superQualifierOffersParentMembers: CjFile) {
        val names = section.collectCandidates(
            superQualifierOffersParentMembers.requestAtReference(TARGET),
        ).lookupNames()

        assertTrue("inherited" in names, "super 之后必须给出父类成员，实际 $names")
    }

    /**
     * 未导入 receiver：按精确同名恢复类型，成员候选照常给出，并带上接收者类型的导入。
     *
     * 只插入 `Widget.size` 而不导入 `Widget`，得到的是一段编译不过的代码——
     * 所以「恢复成员」与「让接收者可解析」必须一起做。
     */
    @Test
    fun recoversMembersOfUnimportedReceiver(recoversMembersOfUnimportedReceiver: CjFile) {
        val snapshots = section.collectCandidates(
            recoversMembersOfUnimportedReceiver.requestAtReference(TARGET),
        )
        val member = snapshots.singleOrNull { it.lookupString == "size" }

        assertTrue(member != null, "未导入 receiver 的成员必须被恢复出来，实际 ${snapshots.map { it.lookupString }}")
        assertTrue(
            member!!.importPlan.requiresImportWrite,
            "恢复出的成员必须同时带上接收者类型的导入计划，实际 ${member.importPlan}",
        )
    }

    /**
     * 同名类型来自多个包：给不出确定的接收者类型，**不得猜**。
     *
     * 随便挑一个会把用户带到错误的成员集合上，而这比不给候选更难发现。
     */
    @Test
    fun doesNotRecoverAmbiguousReceiver(doesNotRecoverAmbiguousReceiver: CjFile) {
        val names = section.collectCandidates(
            doesNotRecoverAmbiguousReceiver.requestAtReference(TARGET),
        ).lookupNames()

        assertTrue(names.isEmpty(), "接收者类型有歧义时不得给出任何成员候选，实际 $names")
    }

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

    /**
     * 约束不满足的 extend 成员不得出现；同一个 extend 在约束满足时必须出现。
     *
     * 只测「满足」的那半不够：约束检查若整段失效，满足的一半照样绿。
     */
    @Test
    fun memberCandidatesExcludeConstraintFailingExtend(mainFile: CjFile) {
        val satisfied = section.collectCandidates(mainFile.requestAtReference(SATISFIED_TARGET)).lookupNames()
        assertTrue(
            satisfied.contains("shapedOnly"),
            "约束满足时 extend 成员必须出现，实际 $satisfied",
        )

        val failing = section.collectCandidates(mainFile.requestAtReference(FAILING_TARGET)).lookupNames()
        assertFalse(
            failing.contains("shapedOnly"),
            "约束不满足时 extend 成员不得出现，实际 $failing",
        )
    }

    /**
     * private 的 extend 成员不得出现；同 extend 里的公开成员必须出现。
     *
     * 权限与约束是两条独立的适用性判据，各自都要有反例——
     * 只测其中一条时，另一条整段失效不会被任何用例发现。
     */
    @Test
    fun memberCandidatesExcludePrivateExtend(mainFile: CjFile) {
        val names = section.collectCandidates(mainFile.requestAtReference(TARGET)).lookupNames()

        assertFalse(names.contains("secret"), "private extend 成员不得出现，实际 $names")
        assertTrue(names.contains("open"), "同 extend 里的公开成员必须出现，实际 $names")
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

        /** 约束满足位置（`Box<Shaped>`）。 */
        private const val SATISFIED_TARGET = "satisfiedTarget"

        /** 约束不满足位置（`Box<Plain>`）。 */
        private const val FAILING_TARGET = "failingTarget"
    }
}
