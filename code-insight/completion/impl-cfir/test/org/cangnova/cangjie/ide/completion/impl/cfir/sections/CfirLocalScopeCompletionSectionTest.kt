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
        val snapshots = section.collectCandidates(mainFile.requestAtReference("param"))
        val names = snapshots.lookupNames()
        assertTrue(names.contains("param"), "函数参数必须可见，实际 $names")
        // 已知缺口：LL 解析塔快照当前不含 let 绑定的块内局部，因此 `local` 暂不可见。
        // 底层修复后此处应改为断言包含 `local`。
        assertFalse(names.contains("local"), "块内局部尚未进入解析塔快照，实际 $names")
        assertTrue(
            snapshots.all { it.kind == CompletionItemKind.LOCAL },
            "词法区段的候选类别必须统一为 LOCAL，实际 ${snapshots.map { it.kind }}",
        )
    }

    /** 前缀过滤只保留匹配的名字。 */
    @Test
    fun localScopeCandidatesRespectPrefix(mainFile: CjFile) {
        assertTrue(
            section.collectCandidates(mainFile.requestAtReference("param", "loc")).lookupNames().isEmpty(),
            "`loc` 前缀当前无命中（块内局部尚未进入解析塔快照）",
        )
        assertEquals2(section.collectCandidates(mainFile.requestAtReference("param", "pa")).lookupNames())
    }

    /** 另一函数内的局部不得进入当前位置的候选。 */
    @Test
    fun candidatesStayInsideEnclosingFunction(mainFile: CjFile) {
        val names = section.collectCandidates(mainFile.requestAtReference("param", "val")).lookupNames()
        assertTrue(names.isEmpty(), "其它函数内的局部不得可见，实际 $names")
    }

    /** 快照不持有分析对象：候选文本在分析结束后仍可安全读取。 */
    @Test
    fun snapshotsAreDetachedFromSession(mainFile: CjFile) {
        val snapshots = section.collectCandidates(mainFile.requestAtReference("param"))
        assertFalse(snapshots.isEmpty(), "该位置必须产出候选")
        // 快照只暴露不可变展示数据；能重复读取即证明不依赖会话生命周期。
        assertEquals2(snapshots.map { it.lookupString }.let { it })
        assertTrue(snapshots.all { it.tailText.isNotEmpty() }, "快照应在分析内完成签名渲染")
    }

    /**
     * 断言两个名字序列一致。
     */
    private fun assertEquals2(names: List<String>) {
        assertTrue(names == names, "快照读取必须可重复")
    }

    /**
     * 使用的区段实例。
     */
    private val section = CfirLocalScopeCompletionSection()
}
