package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 仓颉命名实参候选回归。
 *
 * 覆盖三件事：候选来自被调用方的**命名形参**；本调用已用过的名字被排除；
 * 前缀是仓颉的 `name: ` 而**不是** Kotlin 的 `name = `。
 */
@OptIn(CaExperimentalApi::class)
class CfirNamedArgumentCompletionSectionTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 候选是被调用方尚未使用的命名形参。
     *
     * 夹具里 `host(gamma, alpha!, beta!)`，调用点已写 `alpha: 2`，
     * 因此 `alpha` 必须缺席、`beta` 必须出现；无名形参 `gamma` 永远不是命名实参候选。
     */
    @Test
    fun namedArgumentCandidatesExcludeUsedNames(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(TARGET))
        val names = snapshots.lookupNames()

        assertTrue("beta" in names, "未使用的命名形参必须可见，实际 $names")
        assertFalse("alpha" in names, "本调用已用过的名字不得再出现，实际 $names")
        assertFalse("gamma" in names, "无名形参不得作为命名实参候选，实际 $names")

        assertTrue(
            snapshots.all { it.kind == CompletionItemKind.NAMED_ARGUMENT },
            "命名实参候选类别必须统一，实际 ${snapshots.map { it.kind }}",
        )
    }

    /**
     * 前缀必须是仓颉的 `name: `，不得是 Kotlin 的 `name = `。
     */
    @Test
    fun namedArgumentUsesCangjieColonPrefix(mainFile: CjFile) {
        val snapshot = section.collectCandidates(mainFile.requestAtReference(TARGET))
            .single { it.lookupString == "beta" }

        assertEquals("beta: ", snapshot.insertion.namedArgumentPrefix)
        assertFalse(
            snapshot.insertion.namedArgumentPrefix.contains("="),
            "仓颉命名实参不使用等号形式，实际 ${snapshot.insertion.namedArgumentPrefix}",
        )
    }

    /**
     * 非命名实参位置不得产出候选。
     */
    @Test
    fun namedArgumentAbsentOutsideCallArgument(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference(TARGET))
        assertTrue(
            snapshots.all { it.kind != CompletionItemKind.NAMED_ARGUMENT },
            "普通表达式位置不得出现命名实参候选，实际 ${snapshots.map { it.kind to it.lookupString }}",
        )
    }

    /**
     * 使用的区段实例。
     */
    private val section = CfirNamedArgumentCompletionSection()

    companion object {
        /** 夹具中待补全的标识符。 */
        private const val TARGET = "completionTarget"
    }
}