package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CDoc 注释内候选回归。
 *
 * 三种形态各一条：`@` 后的标签名、`@param [` 内的参数名、`@see [` 内的声明引用。
 * 它们共用「文档位置」这一分类，但候选来源完全不同——标签来自语言、参数来自所属声明、
 * 引用来自作用域。
 */
@OptIn(CaExperimentalApi::class)
class CfirCDocCompletionSectionTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * `@` 之后给出标签名，且**不带 `@`**——光标前的 `@` 已经写下了。
     */
    @Test
    fun cdocOffersTagNames(cdocOffersTagNames: CjFile) {
        val snapshots = section.collectCandidates(requestAfter(cdocOffersTagNames, "@par", prefix = "par"))
        val names = snapshots.map { it.lookupString }

        assertTrue("param" in names, "标签名必须可见，实际 $names")
        assertFalse(names.any { it.startsWith("@") }, "插入文本不得自带 @，实际 $names")
        assertTrue(
            snapshots.all { it.kind == CompletionItemKind.DOCUMENTATION },
            "文档位置候选的类别必须统一，实际 ${snapshots.map { it.kind }}",
        )
        assertTrue(
            snapshots.all { it.identity.kind == CompletionIdentityKind.DOCUMENTATION },
            "文档候选的身份类别必须是 DOCUMENTATION（复查里定义为永不失效）",
        )
    }

    /**
     * 已经写过的标签不再给：同一个标签写两遍没有意义。
     */
    @Test
    fun cdocExcludesAlreadyUsedTags(cdocExcludesAlreadyUsedTags: CjFile) {
        val names = section.collectCandidates(requestAfter(cdocExcludesAlreadyUsedTags, "@\n", prefix = ""))
            .map { it.lookupString }

        assertFalse("param" in names, "已出现的标签不得重复给出，实际 $names")
        assertTrue("return" in names, "未出现的标签仍应给出，实际 $names")
    }

    /**
     * `@param [` 内给出**所属声明的形参名**，不是全文件的名字。
     */
    @Test
    fun cdocParamOffersParameterNames(cdocParamOffersParameterNames: CjFile) {
        val names = section.collectCandidates(
            requestAfter(cdocParamOffersParameterNames, "@param [va", prefix = "va"),
        ).map { it.lookupString }

        assertEquals(listOf("value"), names, "只应给出所属声明的形参名")
    }

    /**
     * `@see [` 内给出作用域内的声明引用。
     */
    @Test
    fun cdocSeeOffersDeclarations(cdocSeeOffersDeclarations: CjFile) {
        val snapshots = section.collectCandidates(
            requestAfter(cdocSeeOffersDeclarations, "@see [Doc", prefix = "Doc"),
        )

        assertTrue(
            snapshots.any { it.lookupString == "Document" },
            "引用标签内必须给出作用域声明，实际 ${snapshots.map { it.lookupString }}",
        )
    }

    /**
     * 非文档位置不产出任何候选。
     */
    @Test
    fun cdocAbsentOutsideDocumentation(cdocAbsentOutsideDocumentation: CjFile) {
        val snapshots = section.collectCandidates(cdocAbsentOutsideDocumentation.requestAtReference("value"))

        assertTrue(snapshots.isEmpty(), "代码位置不得产出文档候选，实际 ${snapshots.map { it.lookupString }}")
    }

    /**
     * 在注释内 `marker` 之后构造补全请求。
     *
     * 注释里没有名字表达式，`requestAtReference` 用不上；而且文档注释由 CDoc 词法产出
     * `CDocElement`，**不是 `CjElement`**。因此分析位置给文件，位置分类改走
     * [CompletionRequest.positionElement]（光标处的原始叶子）。
     */
    private fun requestAfter(file: CjFile, marker: String, prefix: String): CompletionRequest {
        val comment = PsiTreeUtil.findChildrenOfType(file, PsiComment::class.java).first()
        val offsetInComment = comment.text.indexOf(marker)
        require(offsetInComment >= 0) { "夹具必须含标记 `$marker`" }
        val caret = offsetInComment + marker.length
        val caretElement: PsiElement = comment.findElementAt(caret) ?: comment
        return TestCompletionRequest(position = file, prefix = prefix, positionElement = caretElement)
    }

    /**
     * 使用的区段实例。
     */
    private val section = CfirCDocCompletionSection()
}
