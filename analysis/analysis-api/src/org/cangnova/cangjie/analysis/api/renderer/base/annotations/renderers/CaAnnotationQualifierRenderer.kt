package org.cangnova.cangjie.analysis.api.renderer.base.annotations.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRenderer
import org.cangnova.cangjie.psi.CjBuiltInAnnotation

/**
 * 注解限定名 renderer。
 *
 * 仓颉注解有两条明确语义线：
 * - 内置注解：永远按语言级短名输出
 * - 自定义注解：再根据 preset 选择短名或限定名
 *
 * 这里严格只使用 analysis-api 已公开的注解身份字段：
 * `shortName` 和 `classId`。
 * 如果两者都缺失，说明上游注解语义模型本身不完整，而不是 renderer 应该回退到文本切片。
 */
fun interface CaAnnotationQualifierRenderer {
    fun renderQualifier(
        analysisSession: CaSession,
        annotation: CaAnnotation,
        owner: CaAnnotated,
        annotationRenderer: CaAnnotationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val WITH_QUALIFIED_NAMES: CaAnnotationQualifierRenderer = CaAnnotationQualifierRenderer { _, annotation, _, _, printer ->
            printer.append(annotation.renderQualifier(useQualifiedNameForCustomAnnotations = true))
        }

        val WITH_SHORT_NAMES: CaAnnotationQualifierRenderer = CaAnnotationQualifierRenderer { _, annotation, _, _, printer ->
            printer.append(annotation.renderQualifier(useQualifiedNameForCustomAnnotations = false))
        }

        private fun CaAnnotation.renderQualifier(useQualifiedNameForCustomAnnotations: Boolean): String {
            val classId = classId
            val stableShortName = shortName?.asString()
                ?: classId?.shortClassName?.asString()
                ?: error("CaAnnotationRenderer 需要稳定的 shortName 或 classId 才能渲染注解名。")

            return when {
                CjBuiltInAnnotation.isBuiltIn(stableShortName) -> stableShortName
                useQualifiedNameForCustomAnnotations && classId != null -> classId.asSingleFqName().asString()
                else -> stableShortName
            }
        }
    }
}
