package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.containingDeclarationProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.findUsageSimpleName
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiContainingDeclarationTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.containingDeclarationTargetName
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * containing declaration by reference 抽象测试。
 *
 * 这条链路锁定的是：
 * 1. 引用恢复出的 symbol 是否稳定；
 * 2. `containingDeclaration` 是否形成正确的语义容器链；
 * 3. 局部声明 / extend 成员 / 顶层声明在容器建模上是否一致。
 */
abstract class AbstractContainingDeclarationProviderByReferenceTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiContainingDeclarationTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val referenceExpression = findUsageSimpleName(mainFile, directives.containingDeclarationTargetName)

        analyzeForTest(referenceExpression) {
            val symbol = referenceExpression.resolveToSymbol() ?: error("Reference is not resolved")
            val actual = generateSequence(symbol) { current ->
                (current as? CaDeclarationSymbol)?.containingDeclaration
            }
                .filterIsInstance<CaDeclarationSymbol>()
                .joinToString("\n") { render(it) }

            val expected = directives[AnalysisApiContainingDeclarationTestDirectives.EXPECTED_CONTAINING_DECLARATION]
                .joinToString("\n")
            assertEquals(expected, actual)
        }
    }

    private fun render(symbol: CaSymbol): String {
        val rendered = when (symbol) {
            is CaCallableSymbol -> {
                val extendContainer = symbol.containingDeclaration as? CaExtendSymbol
                if (extendContainer != null) {
                    val packageName = extendContainer.extendId.substringBefore(':')
                    val receiverType = extendContainer.extendId.substringAfter(':').substringBefore("<:")
                    "$packageName.$receiverType.${symbol.name?.asString() ?: "Unnamed"}"
                } else {
                    symbol.callableId?.toString() ?: symbol.name?.asString() ?: "Unnamed"
                }
            }
            is CaClassLikeSymbol -> symbol.classId?.toString() ?: symbol.name?.asString() ?: "Unnamed"
            is CaExtendSymbol -> symbol.extendId
            else -> symbol.name?.asString() ?: "Unnamed"
        }
        return rendered.replace('/', '.')
    }
}
