package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.references.CfirNamedReference
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

internal fun CfirAnnotation.shortNameOrNull(): Name? {
    val classId = CfirExtendSemantics.run { typeRef.toClassIdOrNull() }
    if (classId != null) return classId.shortClassName
    (this as? CfirAnnotationCall)
        ?.calleeReference
        ?.let { it as? CfirNamedReference }
        ?.name
        ?.let { return it }
    val sourceText = source?.text?.toString()?.trim().orEmpty()
    if (!sourceText.startsWith("@")) return null
    return Name.identifierIfValid(
        sourceText.removePrefix("@!")
            .removePrefix("@")
            .substringBefore('[')
            .substringBefore('(')
            .substringAfterLast('.')
            .trim(),
    )
}

internal fun CfirAnnotationCall.argumentTextAt(index: Int): String? {
    val argument = argumentList.arguments.getOrNull(index) ?: return null
    (argument as? CfirLiteralExpression)?.value?.let { return it.toString() }
    return argument.source?.text?.toString()
        ?.substringAfter(':')
        ?.trim()
        ?.trim('"')
        ?.takeIf(String::isNotEmpty)
}

internal fun CfirAnnotationCall.argumentCount(): Int =
    argumentList.arguments.size

internal fun CfirAnnotationCall.hasArguments(): Boolean =
    argumentCount() > 0

internal fun CfirAnnotationCall.hasNamedArgument(name: String): Boolean =
    argumentByName(name) != null || rawArgumentText().contains("$name:")

internal fun CfirAnnotationCall.firstArgumentIsNamed(): Boolean {
    val firstArgumentText = rawArgumentTexts().firstOrNull() ?: return false
    return firstArgumentText.substringBefore('[').contains(':')
}

internal fun CfirAnnotationCall.rawNamedArgumentNames(): List<String> =
    rawArgumentTexts()
        .mapNotNull { text ->
            text.substringBefore(':', missingDelimiterValue = "")
                .trim()
                .takeIf(String::isNotEmpty)
        }

internal fun CfirAnnotationCall.argumentByName(name: String): CfirExpression? {
    val resolved = argumentList as? CfirResolvedArgumentList ?: return null
    return resolved.mapping.entries.firstOrNull { (_, parameter) -> parameter.name.asString() == name }?.key
}

internal fun CfirAnnotationCall.namedArgumentText(name: String): String? {
    argumentByName(name)?.let { argument ->
        (argument as? CfirLiteralExpression)?.value?.let { return it.toString() }
        return argument.source?.text?.toString()
            ?.substringAfter(':')
            ?.trim()
            ?.trim('"')
            ?.takeIf(String::isNotEmpty)
    }
    val rawText = rawArgumentText()
    val marker = "$name:"
    val index = rawText.indexOf(marker)
    if (index < 0) return null
    return rawText.substring(index + marker.length)
        .substringBefore(',')
        .substringBefore(']')
        .trim()
        .trim('"')
        .takeIf(String::isNotEmpty)
}

internal fun CfirAnnotationCall.argumentsAreLiteralLike(): Boolean =
    argumentList.arguments.all { it.isAnnotationLiteralLike() }

internal fun CfirAnnotationCall.firstArgumentIsBooleanLiteralNamed(name: String): Boolean {
    val argument = argumentByName(name) ?: argumentList.arguments.firstOrNull() ?: return false
    val raw = argument.source?.text?.toString()?.substringAfter(':')?.trim() ?: argumentTextAt(0)
    return raw == "true" || raw == "false"
}

internal fun CfirExpression.isAnnotationLiteralLike(): Boolean = when (this) {
    is CfirLiteralExpression -> true
    is CfirArrayLiteral -> elements.all { it.isAnnotationLiteralLike() }
    else -> false
}

internal fun CfirExpression.literalStringOrNull(): String? =
    (this as? CfirLiteralExpression)?.value?.toString()

internal fun CfirAnnotationCall.booleanArgument(name: String): Boolean? {
    for ((argument, parameter) in (argumentList as? CfirResolvedArgumentList)?.mapping.orEmpty()) {
        if (parameter.name.asString() != name) continue
        val literal = argument as? CfirLiteralExpression
        if (literal?.value is Boolean) return literal.value as Boolean
        return argument.source?.text?.toString()?.substringAfter(':')?.trim()?.toBooleanStrictOrNull()
    }

    val rawText = source?.text?.toString().orEmpty()
    val marker = "$name:"
    val index = rawText.indexOf(marker)
    if (index < 0) return null
    return rawText.substring(index + marker.length)
        .substringBefore(',')
        .substringBefore(']')
        .trim()
        .toBooleanStrictOrNull()
}

private fun CfirAnnotation.matchesAnnotationName(annotationName: Name): Boolean {
    return shortNameOrNull() == annotationName
}

private fun CfirAnnotationCall.rawArgumentText(): String =
    source?.text?.toString()
        ?.substringAfter('[', missingDelimiterValue = "")
        ?.substringBeforeLast(']', missingDelimiterValue = "")
        .orEmpty()

private fun CfirAnnotationCall.rawArgumentTexts(): List<String> =
    rawArgumentText()
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
