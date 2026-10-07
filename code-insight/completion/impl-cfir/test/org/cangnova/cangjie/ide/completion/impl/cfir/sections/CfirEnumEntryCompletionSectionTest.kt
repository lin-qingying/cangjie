package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 枚举值候选回归。
 *
 * 覆盖三件事：同名不同 payload 个数的构造器**都在**；候选类别是 `ENUM_ENTRY`；
 * 非枚举成员访问处不产出枚举值候选。
 */
@OptIn(CaExperimentalApi::class)
class CfirEnumEntryCompletionSectionTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 同名不同参数个数的枚举构造器必须各自保留。
     *
     * 仓颉允许 `| Dot` 与 `| Dot(Int64)` 并存（cjc 1.1.3 实测合法），
     * 按显示名去重会把两个合法构造器合并成一个，用户无法选到无参那个。
     */
    @Test
    fun enumEntryKeepsSameNameDifferentArity(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(TARGET))
        val names = snapshots.lookupNames()

        assertEquals(2, names.count { it == "Dot" }, "同名不同参数的枚举构造器必须都保留，实际 $names")
        assertTrue("Boxed" in names, "带 payload 的枚举构造器必须可见，实际 $names")
        assertTrue("Dot" in names && names.isNotEmpty(), "无 payload 的枚举值必须可见，实际 $names")

        // 去重必须靠 payload 区分：两个 Dot 的签名尾文本不同。
        val dotTails = snapshots.filter { it.lookupString == "Dot" }.map { it.tailText }
        assertEquals(2, dotTails.distinct().size, "两个 Dot 必须靠 payload 区分，实际尾文本 $dotTails")

        // 身份键同样必须含 payload：按名字去重会让两个合法构造器互相吞掉。
        val dotKeys = snapshots.filter { it.lookupString == "Dot" }.map { it.identity.deduplicationKey }
        assertEquals(2, dotKeys.distinct().size, "两个 Dot 的去重键必须不同，实际 $dotKeys")
        assertEquals(snapshots.size, snapshots.map { it.identity.deduplicationKey }.distinct().size, "全部候选的去重键必须两两不同，否则合法重载会被合并")
    }

    /**
     * 枚举值候选类别统一为 `ENUM_ENTRY`。
     *
     * 枚举构造器在语义上是枚举值而非普通成员，混进 `MEMBER` 会让用户
     * 看不出这是枚举构造器，插入形态与排序权重也都不对。
     */
    @Test
    fun enumEntryCarriesEnumEntryKind(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(TARGET))

        assertEquals(setOf("On", "Off"), snapshots.lookupNames().toSet(), "两个枚举值都应可见")
        assertTrue(
            snapshots.all { it.kind == CompletionItemKind.ENUM_ENTRY },
            "枚举值候选类别必须统一为 ENUM_ENTRY，实际 ${snapshots.map { it.kind to it.lookupString }}",
        )
    }

    /**
     * 非枚举类型的成员访问处不得产出枚举值候选。
     */
    @Test
    fun enumEntryAbsentOutsideEnumMemberAccess(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(TARGET))

        assertTrue(
            snapshots.all { it.kind != CompletionItemKind.ENUM_ENTRY },
            "非枚举接收者不得出现枚举值候选，实际 ${snapshots.map { it.kind to it.lookupString }}",
        )
    }

    /**
     * `match` 模式位置给出主语类型的枚举构造器。
     *
     * 仓颉按主语类型的枚举构造器分派，用户在这里敲的几乎总是其中之一；
     * 而他此刻**没有任何接收者可写**——`Dot` 不像 `Shape.Dot` 那样有个类型名可以打出来。
     */
    @Test
    fun matchPatternOffersEnumConstructors(matchPatternOffersEnumConstructors: CjFile) {
        val snapshots = section.collectCandidates(matchPatternOffersEnumConstructors.requestAtReference(TARGET))
        val names = snapshots.lookupNames()

        assertTrue("Dot" in names, "无参枚举构造器必须可见，实际 $names")
        assertTrue("Boxed" in names, "有参枚举构造器必须可见，实际 $names")
        assertTrue(
            snapshots.all { it.kind == CompletionItemKind.ENUM_ENTRY },
            "模式位置的候选类别必须统一为 ENUM_ENTRY，实际 ${snapshots.map { it.kind }}",
        )
    }

    /**
     * `match` 之外不产出：模式候选只在模式位置有意义。
     */
    @Test
    fun matchPatternAbsentOutsideMatch(matchPatternAbsentOutsideMatch: CjFile) {
        val snapshots = section.collectCandidates(matchPatternAbsentOutsideMatch.requestAtReference(TARGET))

        assertTrue(
            snapshots.none { it.kind == CompletionItemKind.ENUM_ENTRY },
            "普通表达式位置不得出现枚举值候选，实际 ${snapshots.map { it.kind to it.lookupString }}",
        )
    }

    /**
     * 使用的区段实例。
     */
    private val section = CfirEnumEntryCompletionSection()

    companion object {
        /**
         * 夹具中待补全的标识符。
         */
        private const val TARGET = "completionTarget"
    }
}