package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关键字与声明名候选区段回归。
 *
 * 关键字来自语言语法，不来自作用域；声明名来自包内已有符号。
 * 两侧都必须有 **缺席** 断言：无接收者时不得给 `this`，
 * 非声明位置不得给声明关键字，Kotlin 专属关键字永远不出现。
 */
@OptIn(CaExperimentalApi::class)
class CfirKeywordCompletionSectionTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 函数体位置给出仓颉关键字，且不含 Kotlin 专属关键字。 */
    @Test
    fun keywordCandidatesInFunctionBody(mainFile: CjFile) {
        val names = section.collectCandidates(mainFile.requestAtReference(TARGET)).lookupNames()
        assertTrue(names.contains("return"), "函数体位置必须给出 return，实际 $names")
        assertTrue(names.contains("let"), "函数体位置必须给出 let，实际 $names")
        assertTrue(
            names.none { it in KOTLIN_ONLY_KEYWORDS },
            "不得出现 Kotlin 专属关键字 $KOTLIN_ONLY_KEYWORDS，实际 $names",
        )
    }

    /** 成员访问位置给出 `this`。 */
    @Test
    fun keywordCandidatesInMemberAccess(mainFile: CjFile) {
        val names = section.collectCandidates(mainFile.requestAtReference(TARGET)).lookupNames()
        assertTrue(names.contains("this"), "存在隐式接收者时必须给出 this，实际 $names")
    }

    /** 没有接收者的自由函数位置不得给出 `this`。 */
    @Test
    fun keywordCandidatesAbsenceOutsideReceiver(mainFile: CjFile) {
        val names = section.collectCandidates(mainFile.requestAtReference(TARGET)).lookupNames()
        assertFalse(names.contains("this"), "无接收者时不得给出 this，实际 $names")
    }

    /**
     * 声明名位置只给声明关键字。
     *
     * 此处**刻意不**断言类型名建议：在声明标识符上取导入作用域会与正在解析的声明
     * 互相递归（实测 StackOverflowError），类型名建议因此只在类型位置给出。
     */
    @Test
    fun keywordCandidatesAtDeclarationName(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtDeclarationName(TARGET))
        val names = snapshots.lookupNames()
        assertTrue(names.contains("func"), "声明名位置必须给出 func，实际 $names")
        assertTrue(
            snapshots.all { it.kind == CompletionItemKind.KEYWORD },
            "声明名位置只应给关键字，实际 ${snapshots.map { it.kind }}",
        )
        assertFalse(
            names.any { it in KOTLIN_ONLY_KEYWORDS },
            "声明名位置不得出现 Kotlin 专属关键字，实际 $names",
        )
    }

    /**
     * 类型名建议只在类型位置出现。
     *
     * 缺席侧是重点：类型名建议一旦泄漏到非类型位置，用户会在写表达式时收到一串类名，
     * 选中即编译失败。
     */
    @Test
    fun typeSuggestionsAppearOnlyAtTypePosition(mainFile: CjFile) {
        val inBody = section.collectCandidates(mainFile.requestAtReference(TARGET))
        assertTrue(
            inBody.all { it.kind != CompletionItemKind.TOP_LEVEL },
            "函数体位置不得出现类型名建议，实际 ${inBody.map { it.kind to it.lookupString }}",
        )

        val atType = section.collectCandidates(mainFile.requestAtReference("Holder"))
        assertTrue(
            atType.lookupNames().contains("Holder"),
            "类型位置必须给出类型名建议，实际 ${atType.lookupNames()}",
        )
        assertTrue(
            atType.any { it.kind == CompletionItemKind.TOP_LEVEL },
            "类型建议必须标记为顶层来源，实际 ${atType.map { it.kind }}",
        )
    }

    /**
     * `operator` 声明关键字不出现在函数体位置。
     *
     * 缺席侧锁住的是「位置分类错了会把 `operator` 送到普通函数体」——
     * 那会让用户在函数体里收到一个根本无处可放的声明关键字。
     *
     * 正面侧用已有的 `keywordCandidatesAtDeclarationName`（`func` 等）在
     * `keywordCandidatesAtDeclarationName.cj` 夹具上断言；这里不再重复走声明名位置——
     * 那条路径在类型建议场景下会触发 StackOverflowError。
     */
    @Test
    fun operatorKeywordAbsentInFunctionBody(mainFile: CjFile) {
        val inBody = section.collectCandidates(mainFile.requestAtReference(TARGET))
        assertFalse(
            inBody.lookupNames().contains("operator"),
            "函数体位置不得给出 operator，实际 ${inBody.lookupNames()}",
        )
        assertTrue(
            inBody.lookupNames().contains("return"),
            "函数体位置应给出该位置合法的关键字，实际 ${inBody.lookupNames()}",
        )
    }

    /**
     * 使用的区段实例。
     */
    private val section = CfirKeywordCompletionSection()

    companion object {
        /** 夹具中待补全的标识符。 */
        private const val TARGET = "completionTarget"

        /** Kotlin 专属、仓颉不具备的关键字；任何位置都不得出现。 */
        private val KOTLIN_ONLY_KEYWORDS = setOf("suspend", "companion", "by", "crossinline", "noinline", "out", "reified")
    }
}
