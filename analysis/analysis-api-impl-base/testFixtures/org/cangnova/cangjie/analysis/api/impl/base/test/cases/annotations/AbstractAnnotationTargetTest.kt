package org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 注解适用目标（`CaAnnotation.target`）公开 API 的抽象测试。
 *
 * 对齐 Kotlin `AbstractAnnotationTargetTest`：caret 定位声明，恢复声明符号上的注解列表，
 * 渲染每个注解的 `classId` 与其注解类声明的适用目标。目标缺失显式输出 `null`，
 * 保证"未声明 target"与"显式全目标"在 golden 中可区分。
 */
abstract class AbstractAnnotationTargetTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行注解适用目标测试。
     *
     * 方法从 caret 处定位声明，输出声明类别、名称，以及注解列表中每个注解的
     * 完整 ClassId 与 `CangjieAnnotationTarget` 枚举名。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjDeclaration>(mainFile)

        val actual = copyAwareAnalyzeForTest(declaration) { contextDeclaration ->
            val declarationSymbol = contextDeclaration.symbol as? CaDeclarationSymbol
                ?: error("Declaration `${contextDeclaration.text}` does not resolve to a declaration symbol.")
            buildString {
                appendLine("${contextDeclaration::class.simpleName}: ${contextDeclaration.name}")
                appendLine("annotations: [")
                for (annotation in declarationSymbol.annotations) {
                    appendLine("  @${annotation.classId?.asFqNameString() ?: annotation.shortName?.asString() ?: "<unresolved>"}")
                    appendLine("    target: ${annotation.target?.name ?: "null"}")
                }
                appendLine("]")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
