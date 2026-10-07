package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.impl.shared.CompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroAttr
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 宏 token 位置的**上下文分流**。
 *
 * 分流要证明的是「不该给的一个都不给」：宏 token 序列里没有可解析的代码上下文，
 * 任何语义候选（局部、成员、顶层、索引）在那里都是噪音；更不能退化成词补全或
 * 全工作区扫描。同时区段在非仓颉 PSI 的叶子上**不得抛异常**——一个没做位置守卫的
 * 区段在这里抛异常，表现就是「在宏里补全整条管线崩掉」。
 */
@OptIn(CaExperimentalApi::class)
class CfirMacroTokenPositionTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 位置分类确为 `MACRO_TOKENS`，且**每个**区段都返回空且不抛异常。
     */
    @Test
    fun macroTokenPositionYieldsNoSemanticCandidates(macroTokenPositionYieldsNoSemanticCandidates: CjFile) {
        val position = macroTokenLeaf(macroTokenPositionYieldsNoSemanticCandidates)
        // 两侧都是枚举，不存在 String/String 的 message-first 歧义，用位置参数即可。
        assertEquals(
            CompletionPositionKind.MACRO_TOKENS,
            CompletionPositionContexts.classify(position).kind,
            "夹具必须落在属性宏的 token 输入里",
        )

        // 宏 token 叶子不是 CjElement，分析位置给文件，位置分类走 positionElement
        // （与文档注释位置同一处理方式）。
        val request = TestCompletionRequest(
            position = macroTokenPositionYieldsNoSemanticCandidates,
            prefix = "",
            positionElement = position,
        )
        val offending = linkedMapOf<String, List<CompletionSnapshot>>()
        for (section in SECTIONS) {
            val candidates = section.collectCandidates(request)
            if (candidates.isNotEmpty()) offending[section.id] = candidates
        }

        assertTrue(
            offending.isEmpty(),
            "token 位置不得产出任何语义候选，实际 ${offending.mapValues { (_, v) -> v.map { it.lookupString } }}",
        )
    }

    /**
     * 取属性宏方括号内的叶子。
     *
     * `CjMacroAttr` 的内容不是仓颉 PSI（就是 token 序列），因此这里刻意不假设叶子类型，
     * 与产品路径上 `positionElement` 承载原始叶子的语义一致。
     */
    private fun macroTokenLeaf(file: CjFile): PsiElement {
        val attr = PsiTreeUtil.findChildrenOfType(file, CjMacroAttr::class.java).first()
            ?: error("夹具必须含属性宏的方括号输入")
        return attr.findElementAt(1) ?: error("方括号内必须存在叶子元素")
    }

    companion object {
        /**
         * 生产管线里注册的全部区段。刻意手写这份清单而不是从服务里取：
         * 服务入口需要进度指示器（测试线程上为 null），而这里要验证的正是
         * 「**所有**区段在 token 位置上都不产出」——漏掉任何一个都让结论不成立。
         */
        private val SECTIONS: List<CompletionSection> = listOf(
            CfirKeywordCompletionSection(),
            CfirCDocCompletionSection(),
            CfirLocalScopeCompletionSection(),
            CfirMemberCompletionSection(),
            CfirEnumEntryCompletionSection(),
            CfirTopLevelCompletionSection(),
            CfirNamedArgumentCompletionSection(),
            CfirIndexCompletionSection(),
        )
    }
}
