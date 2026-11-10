package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 声明生成建议区段回归。
 *
 * 断言的是**区段与窄接口之间的契约**：候选来自生成服务、类别与身份正确、
 * 尾文本给出渲染后的声明。生成器本身的正确性由 `override-implement` 的用例验证——
 * 两边分开测，区段就不会因为生成器换实现而变红。
 */
@OptIn(CaExperimentalApi::class)
class CfirDeclarationSuggestionCompletionSectionTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 注册生成服务：区段消费的是接口，测试提供它的假实现。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(DeclarationSuggestionTestServiceRegistrar)

    /**
     * 类体位置的候选来自生成服务，类别与身份都是声明建议。
     */
    @Test
    fun declarationSuggestionsComeFromGenerationService(
        declarationSuggestionsComeFromGenerationService: CjFile,
    ) {
        val snapshots = section.collectCandidates(
            declarationSuggestionsComeFromGenerationService.requestAtDeclarationName("Host"),
        )
        val candidate = snapshots.single()

        assertEquals("greet", candidate.lookupString, "候选名取自生成服务")
        assertEquals(
            CompletionItemKind.DECLARATION_SUGGESTION,
            candidate.kind,
            "类别必须是声明建议，否则会被当成真实符号",
        )
        assertEquals(
            CompletionIdentityKind.DECLARATION_SUGGESTION,
            candidate.identity.kind,
            "身份类别必须一致：它不引用已有声明，复查据此判定永不失效",
        )
        assertTrue(
            candidate.tailText.contains("func greet"),
            "尾文本必须给出渲染后的声明，实际 ${candidate.tailText}",
        )
    }

    /**
     * 方法体位置不产出声明建议：那里该给的是普通表达式候选。
     *
     * 判据是「在类体内、且不在成员的方法体里」——方法体里的位置同样是「在类体内」，
     * 只按后者判会让用户在写方法实现时收到一串「要不要生成某个成员」。
     */
    @Test
    fun declarationSuggestionAbsentInsideMemberBody(
        declarationSuggestionAbsentInsideMemberBody: CjFile,
    ) {
        val snapshots = section.collectCandidates(
            declarationSuggestionAbsentInsideMemberBody.requestAtReference(TARGET),
        )

        assertTrue(
            snapshots.none { it.kind == CompletionItemKind.DECLARATION_SUGGESTION },
            "方法体内的位置不得产出声明建议，实际 ${snapshots.map { it.kind to it.lookupString }}",
        )
    }

    /**
     * 使用的区段实例。
     */
    private val section = CfirDeclarationSuggestionCompletionSection()

    companion object {
        /** 方法体夹具中待补全的标识符。 */
        private const val TARGET = "completionTarget"
    }
}
