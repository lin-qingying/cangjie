package org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 声明注解列表公开 API 的抽象测试。
 *
 * 测试通过 caret 定位声明，恢复 `CaDeclarationSymbol`，再渲染 `CaAnnotationList`，
 * 用于验证源码声明上的注解是否能通过公开 Analysis API 稳定暴露。
 */
abstract class AbstractAnalysisApiAnnotationsOnDeclarationsTest : AbstractAnalysisApiComponentTest() {
    /**
     * 渲染声明符号上的注解列表。
     *
     * 子类可以覆盖该方法切换渲染策略，例如在普通注解之外递归展开 meta-annotations。
     */
    open fun renderAnnotations(analysisSession: CaSession, annotations: CaAnnotationList): String {
        return TestAnnotationRenderer.renderAnnotations(analysisSession, annotations)
    }

    /**
     * 执行声明注解测试。
     *
     * 方法从主文件 caret 处定位声明，使用复制感知分析恢复上下文声明符号，并将注解渲染结果写入 golden 输出。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjDeclaration>(mainFile)
        val actual = copyAwareAnalyzeForTest(declaration) { contextDeclaration ->
            val declarationSymbol = contextDeclaration.symbol as? CaDeclarationSymbol
                ?: error("Declaration `${contextDeclaration.text}` does not resolve to a declaration symbol.")
            buildString {
                appendLine("${CjDeclaration::class.simpleName}: ${contextDeclaration::class.simpleName} ${contextDeclaration.name}")
                append(renderAnnotations(useSiteSession, declarationSymbol.annotations))
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
