package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeProvider

import org.cangnova.cangjie.analysis.api.components.type
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 类型引用直查（`CjTypeReference.type`）的抽象测试。
 *
 * 对齐 Kotlin `AbstractTypeByTypeReferenceTest`：caret 定位类型引用，直接调用
 * Analysis API 的 `CjTypeReference.type` 桥接得到公开 `CaType` 并渲染。
 * 与现有 `AbstractTypeReferenceTest`（经 owner returnType 观察）不同，本测试
 * 验证类型引用到类型对象的解析通道本身。
 */
abstract class AbstractTypeByTypeReferenceTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行类型引用直查测试。
     *
     * 方法输出类型引用源文本与解析后的公开类型渲染文本（分隔符统一规范化）。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val typeReference = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjTypeReference>(mainFile)

        val actual = analyzeForTest(typeReference) {
            buildString {
                appendLine("CjTypeReference: ${typeReference.getTypeText()}")
                appendLine(
                    "CaType: ${
                        typeReference.type
                            .render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)
                            .let(::normalizeTypeRendering)
                    }",
                )
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
