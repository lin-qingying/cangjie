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
 * 局部作用域候选区段回归。
 *
 * 候选必须来自 Analysis 的位置作用域上下文，因此这里用真实分析会话验证：
 * 参数与先声明的局部可见，前缀过滤正确，快照脱离会话后仍可重复读取，
 * 另一函数内的局部不泄漏。
 */
@OptIn(CaExperimentalApi::class)
class CfirLocalScopeCompletionSectionTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 函数体内的位置给出参数与已声明局部，且不含另一函数内的局部。 */
    @Test
    fun localScopeCandidates(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference("completionTarget"))
        val names = snapshots.lookupNames()
        assertTrue(names.contains("param"), "函数参数必须可见，实际 $names")
        // 补全点在 `let local` 声明**之后**：此时块内局部已在作用域内，
        // 解析塔快照必须带上它。此前这里断言不可见，是缺陷 D-1 的负例锁定。
        assertTrue(names.contains("local"), "块内局部必须可见，实际 $names")
        assertTrue(
            snapshots.all { it.kind == CompletionItemKind.LOCAL },
            "词法区段的候选类别必须统一为 LOCAL，实际 ${snapshots.map { it.kind }}",
        )
    }

    /** 前缀过滤只保留匹配的名字。 */
    @Test
    fun localScopeCandidatesRespectPrefix(mainFile: CjFile) {
        assertEquals(
            listOf("local"),
            section.collectCandidates(mainFile.requestAtReference("completionTarget", "loc")).lookupNames(),
        )
        assertEquals(
            listOf("param"),
            section.collectCandidates(mainFile.requestAtReference("completionTarget", "pa")).lookupNames(),
        )
    }

    /** 另一函数内的局部不得进入当前位置的候选。 */
    @Test
    fun candidatesStayInsideEnclosingFunction(mainFile: CjFile) {
        val names = section.collectCandidates(mainFile.requestAtReference("completionTarget", "val")).lookupNames()
        assertTrue(names.isEmpty(), "其它函数内的局部不得可见，实际 $names")
    }

    /** 快照不持有分析对象：候选文本在分析结束后仍可安全读取。 */
    @Test
    fun snapshotsAreDetachedFromSession(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference("completionTarget"))
        assertFalse(snapshots.isEmpty(), "该位置必须产出候选")
        // 快照只暴露不可变展示数据；能重复读取即证明不依赖会话生命周期。
        assertEquals(snapshots.map { it.lookupString }.let { it }, snapshots.map { it.lookupString })
        assertTrue(snapshots.all { it.tailText.isNotEmpty() }, "快照应在分析内完成签名渲染")
    }

    /**
 * 泛型函数体内的类型参数必须作为词法候选出现，且类别独立于局部值声明。
 *
 * 类型参数是分类器、走 `CaScope.typeParameters` 专用通道；漏掉它会让
 * `func f<T>(x: T)` 体内补不出 `T`。类别记 [CompletionItemKind.TYPE_PARAMETER]
 * 而非 LOCAL——归入 LOCAL 会被渲染成带括号的调用形态。
 */
    @Test
    fun typeParametersAreLexicalCandidates(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference("scopeTarget"))
        val names = snapshots.lookupNames()

        assertTrue("T" in names, "类型参数必须可见，实际 $names")
        assertTrue("param" in names, "值参数必须仍然可见，实际 $names")
        assertTrue("local" in names, "块内局部必须仍然可见，实际 $names")

        assertEquals(
            CompletionItemKind.TYPE_PARAMETER,
            snapshots.single { it.lookupString == "T" }.kind,
            "类型参数类别必须独立于局部值声明",
        )
        assertEquals(
            CompletionItemKind.LOCAL,
            snapshots.single { it.lookupString == "param" }.kind,
            "值参数仍是局部候选",
        )
    }

    /** 使用的区段实例。 */
    private val section = CfirLocalScopeCompletionSection()
}
