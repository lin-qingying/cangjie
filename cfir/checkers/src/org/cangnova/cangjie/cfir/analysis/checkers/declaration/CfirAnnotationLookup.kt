package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.text

/**
 * 统一从 CFIR 注解节点本身识别注解名。
 *
 * Diagnostics2 这类 FRONTEND 测试里，声明 source 不保证总能回到 PSI，
 * 因此 interop / builtin 注解语义不能依赖 `source?.psi` 才能工作。
 */
internal fun CfirDeclaration.hasAnnotation(annotationName: Name): Boolean =
    findAnnotations(annotationName).isNotEmpty()

internal fun CfirDeclaration.findAnnotations(annotationName: Name): List<CfirAnnotation> =
    annotations.filter { annotation -> annotation.matchesAnnotationName(annotationName) }

private fun CfirAnnotation.matchesAnnotationName(annotationName: Name): Boolean {
    val classId = CfirExtendSemantics.run { typeRef.toClassIdOrNull() }
    if (classId?.shortClassName == annotationName) return true

    val sourceText = source?.text?.toString() ?: return false
    return sourceText.startsWith("@${annotationName.asString()}")
}
