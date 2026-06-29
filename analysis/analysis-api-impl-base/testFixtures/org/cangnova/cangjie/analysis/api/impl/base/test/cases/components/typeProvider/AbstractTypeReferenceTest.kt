package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `typeProvider.typeReference` 的抽象测试。
 *
 * 该测试通过 caret 处类型引用所属 callable 的 return type 观察公开类型引用解析结果。
 */
abstract class AbstractTypeReferenceTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行类型引用快照测试。
     *
     * 方法定位 `CjTypeReference`，通过 owner callable symbol 输出对应公开 `CaType` 文本。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val typeReference = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjTypeReference>(mainFile)

        val actual = analyzeForTest(typeReference) {
            val owner = PsiTreeUtil.getParentOfType(typeReference, CjCallableDeclaration::class.java)
            val ownerSymbol = owner?.symbol as? CaCallableSymbol
            buildString {
                appendLine("CjTypeReference: ${typeReference.getTypeText()}")
                appendLine(
                    "CaType: ${
                        ownerSymbol?.returnType
                            ?.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)
                            ?.let(::normalizeTypeRendering)
                    }",
                )
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
