package org.cangnova.cangjie.ide.completion.impl.cfir.filter

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CaCompletionExpectedType
import org.cangnova.cangjie.ide.completion.api.CaCompletionExpectedTypeKind
import org.cangnova.cangjie.ide.completion.api.CaCompletionExpectedTypeProvider
import org.cangnova.cangjie.ide.completion.api.CaCompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CaCompletionKind
import org.cangnova.cangjie.ide.completion.api.CaCompletionRequest
import org.cangnova.cangjie.ide.completion.api.CaCompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CaExpectedTypeMatch
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CaTestCompletionRequest
import org.cangnova.cangjie.ide.completion.impl.shared.caCompletionSnapshot
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 期望类型过滤器回归。
 *
 * 三条接口契约在此固定：
 * 1. BASIC 不查询期望类型、原样返回；
 * 2. SMART 在期望类型未知时返回与 BASIC 相同的集合（同一实例，未重写）；
 * 3. 过滤只依据期望类型结论，触发次数不参与。
 *
 * 期望类型提供者以假实现注入：本测试验证过滤与构造逻辑，
 * Analysis 侧的期望类型推导已由 ExpectedExpressionType 套件覆盖。
 */
@OptIn(CaExperimentalApi::class)
class CfirExpectedTypeCompletionFilterTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/filter",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * BASIC 不得查询期望类型：基础补全的语义是「给出全部适用候选并排序」。
     */
    @Test
    fun basicPassesThrough(mainFile: CjFile) {
        val snapshots = listOf(symbolSnapshot("alpha"), symbolSnapshot("beta"))
        val request = requestOf(mainFile, CaCompletionKind.BASIC)
        val result = filterWith(providerThatMustNotBeCalled()).apply(request, snapshots)
        assertSame(snapshots, result, "BASIC 必须原样返回同一集合")
    }

    /**
     * SMART 在期望类型未知时返回与 BASIC 相同的集合，而不是清空候选。
     */
    @Test
    fun smartKeepsBasicSetWhenExpectedTypeUnknown(mainFile: CjFile) {
        val snapshots = listOf(symbolSnapshot("alpha"), symbolSnapshot("beta"))
        val request = requestOf(mainFile, CaCompletionKind.SMART)
        val result = filterWith(providerReturning(CaCompletionExpectedType.None)).apply(request, snapshots)
        assertSame(snapshots, result, "期望类型未知时必须原样返回同一集合")
    }

    /**
     * 类型位置只保留类型身份候选；触发次数不参与过滤。
     */
    @Test
    fun smartFiltersAtTypePosition(mainFile: CjFile) {
        val type = typeSnapshot("Alpha")
        val value = symbolSnapshot("alpha")
        val filter = filterWith(providerReturning(CaCompletionExpectedType(CaCompletionExpectedTypeKind.TYPE)))

        val request = requestOf(mainFile, CaCompletionKind.SMART)
        assertEquals(
            listOf("Alpha"),
            filter.apply(request, listOf(type, value)).map { it.lookupString },
            "类型位置必须只保留类型身份候选",
        )

        // 重复按 Tab 不构成访问权限：触发次数不得放宽或改变过滤结论。
        val repeated = requestOf(mainFile, CaCompletionKind.SMART, invocationCount = 5)
        assertEquals(
            listOf("Alpha"),
            filter.apply(repeated, listOf(type, value)).map { it.lookupString },
            "触发次数不得影响过滤结论",
        )
    }

    /**
     * 表达式位置：确定匹配加权并保留，判不出类型的候选保持 UNKNOWN 且保留。
     */
    @Test
    fun smartExpressionMatchPromotesWeight(mainFile: CjFile) {
        val matched = symbolSnapshot("alpha", typeText = "Alpha")
        val undecided = symbolSnapshot("beta", typeText = "Beta")
        val noType = symbolSnapshot("gamma")
        val request = requestOf(mainFile, CaCompletionKind.SMART)
        val filter = filterWith(
            providerReturning(CaCompletionExpectedType(CaCompletionExpectedTypeKind.EXPRESSION, "Alpha")),
        )

        val result = filter.apply(request, listOf(matched, undecided, noType))
        assertEquals(
            setOf("alpha", "beta", "gamma"),
            result.map { it.lookupString }.toSet(),
            "判不出类型的候选不得被删除",
        )

        val alpha = result.single { it.lookupString == "alpha" }
        assertEquals(CaExpectedTypeMatch.MATCH, alpha.sort.expectedTypeMatch)
        assertTrue(alpha.weight > matched.weight, "匹配候选必须按新结论重算权重")
        assertEquals(
            CaExpectedTypeMatch.UNKNOWN,
            result.single { it.lookupString == "beta" }.sort.expectedTypeMatch,
            "类型文本不一致时不得武断判为不匹配",
        )
        assertEquals(
            CaExpectedTypeMatch.UNKNOWN,
            result.single { it.lookupString == "gamma" }.sort.expectedTypeMatch,
            "没有类型文本的候选必须保持未知",
        )
    }

    /**
     * 在夹具的锚点引用处构造请求。
     */
    private fun requestOf(mainFile: CjFile, kind: CaCompletionKind, invocationCount: Int = 1): CaCompletionRequest =
        CaTestCompletionRequest(
            position = mainFile.anchorPosition(),
            prefix = "",
            kind = kind,
            invocationCount = invocationCount,
        )

    /**
     * 取得夹具中待补全的标识符。
     */
    private fun CjFile.anchorPosition(): CjElement =
        PsiTreeUtil.findChildrenOfType(this, CjSimpleNameExpression::class.java)
            .last { it.referencedName == ANCHOR }

    /**
     * 构造过滤器，注入假提供者。
     */
    private fun filterWith(provider: CaCompletionExpectedTypeProvider): CfirExpectedTypeCompletionFilter =
        CfirExpectedTypeCompletionFilter(expectedTypeProvider = provider)

    /**
     * BASIC 下绝不允许被调用的提供者。
     */
    private fun providerThatMustNotBeCalled(): CaCompletionExpectedTypeProvider =
        object : CaCompletionExpectedTypeProvider {
            override fun expectedTypeAt(request: CaCompletionRequest): CaCompletionExpectedType =
                error("BASIC 不得查询期望类型")
        }

    /**
     * 固定返回给定结论的提供者。
     */
    private fun providerReturning(expected: CaCompletionExpectedType): CaCompletionExpectedTypeProvider =
        object : CaCompletionExpectedTypeProvider {
            override fun expectedTypeAt(request: CaCompletionRequest): CaCompletionExpectedType = expected
        }

    /**
     * 符号候选：局部来源，身份为 SYMBOL。
     */
    private fun symbolSnapshot(name: String, typeText: String = ""): CaCompletionSnapshot = caCompletionSnapshot(
        lookupString = name,
        kind = CaCompletionItemKind.LOCAL,
        origin = CaCompletionOrigin.LOCAL_SCOPE,
        typeText = typeText,
    ).build()

    /**
     * 类型候选：顶层来源，身份为 TYPE。
     */
    private fun typeSnapshot(name: String): CaCompletionSnapshot = caCompletionSnapshot(
        lookupString = name,
        kind = CaCompletionItemKind.TOP_LEVEL,
        origin = CaCompletionOrigin.TOP_LEVEL,
        identityKind = CaCompletionIdentityKind.TYPE,
    ).build()

    companion object {
        /** 夹具中待补全的标识符。 */
        private const val ANCHOR = "completionTarget"
    }
}
