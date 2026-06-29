package org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `CaType.annotations` 的抽象测试。
 *
 * 当前仓颉 public Analysis API 还没有暴露 `CjTypeReference -> CaType` 的直接入口，
 * 因此这里沿用公开 callable symbol 的 `returnType` 作为类型观察面，
 * 只验证已经公开的 `CaType.annotations` 契约，不发明新的测试桥接 API。
 */
abstract class AbstractAnalysisApiAnnotationsOnTypesTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行类型注解测试。
     *
     * 方法在 caret 处定位 `CjTypeReference`，通过其所属 callable 的公开 return type 观察
     * `CaType.annotations`，并将稳定渲染结果与 golden 文件比较。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val typeReference = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjTypeReference>(mainFile)

        val actual = copyAwareAnalyzeForTest(typeReference) { contextTypeReference ->
            val type = resolveCallableReturnType(contextTypeReference)
            buildString {
                appendLine("${CjTypeReference::class.simpleName}: ${contextTypeReference.text}")
                append(TestAnnotationRenderer.renderAnnotations(useSiteSession, type.annotations))
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    /**
     * 通过类型引用所属 callable 的公开 return type 恢复 `CaType`。
     *
     * 当前测试只使用已经存在的公开 API 面，不直接访问内部类型解析结构。
     */
    private fun CaSession.resolveCallableReturnType(typeReference: CjTypeReference): CaType {
        val owner = PsiTreeUtil.getParentOfType(typeReference, CjCallableDeclaration::class.java)
            ?: error("Type reference `${typeReference.text}` is not owned by a callable declaration.")
        val ownerSymbol = owner.symbol as? CaCallableSymbol
            ?: error("Callable declaration `${owner.text}` does not resolve to a callable symbol.")
        return ownerSymbol.returnType
    }
}
