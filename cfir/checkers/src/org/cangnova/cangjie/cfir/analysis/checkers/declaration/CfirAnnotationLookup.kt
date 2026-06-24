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

/** 查找声明上短名匹配指定注解名的全部注解。 */
internal fun CfirDeclaration.findAnnotations(annotationName: Name): List<CfirAnnotation> =
    annotations.filter { annotation -> annotation.matchesAnnotationName(annotationName) }

/** 从注解 typeRef、calleeReference 或源码文本中提取注解短名。 */
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

/** 返回注解调用指定位置实参的源码文本或字面量文本。 */
internal fun CfirAnnotationCall.argumentTextAt(index: Int): String? {
    val argument = argumentList.arguments.getOrNull(index) ?: return null
    (argument as? CfirLiteralExpression)?.value?.let { return it.toString() }
    return argument.source?.text?.toString()
        ?.substringAfter(':')
        ?.trim()
        ?.trim('"')
        ?.takeIf(String::isNotEmpty)
}

/** 返回注解调用的实参数量。 */
internal fun CfirAnnotationCall.argumentCount(): Int =
    argumentList.arguments.size

/** 判断注解调用是否携带任何实参。 */
internal fun CfirAnnotationCall.hasArguments(): Boolean =
    argumentCount() > 0

/** 判断注解调用是否存在指定名称的命名实参。 */
internal fun CfirAnnotationCall.hasNamedArgument(name: String): Boolean =
    argumentByName(name) != null || rawArgumentText().contains("$name:")

/** 判断第一个注解实参是否使用命名参数形式。 */
internal fun CfirAnnotationCall.firstArgumentIsNamed(): Boolean {
    val firstArgumentText = rawArgumentTexts().firstOrNull() ?: return false
    return firstArgumentText.substringBefore('[').contains(':')
}

/** 从注解源码文本中提取所有命名实参名。 */
internal fun CfirAnnotationCall.rawNamedArgumentNames(): List<String> =
    rawArgumentTexts()
        .mapNotNull { text ->
            text.substringBefore(':', missingDelimiterValue = "")
                .trim()
                .takeIf(String::isNotEmpty)
        }

/** 根据已解析实参映射查找指定名称对应的实参表达式。 */
internal fun CfirAnnotationCall.argumentByName(name: String): CfirExpression? {
    val resolved = argumentList as? CfirResolvedArgumentList ?: return null
    return resolved.mapping.entries.firstOrNull { (_, parameter) -> parameter.name.asString() == name }?.key
}

/** 返回指定命名实参的字面量文本或源码文本。 */
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

/** 判断注解调用的所有实参是否都是字面量或字面量数组形态。 */
internal fun CfirAnnotationCall.argumentsAreLiteralLike(): Boolean =
    argumentList.arguments.all { it.isAnnotationLiteralLike() }

/** 判断第一个实参或指定命名实参是否为布尔字面量。 */
internal fun CfirAnnotationCall.firstArgumentIsBooleanLiteralNamed(name: String): Boolean {
    val argument = argumentByName(name) ?: argumentList.arguments.firstOrNull() ?: return false
    val raw = argument.source?.text?.toString()?.substringAfter(':')?.trim() ?: argumentTextAt(0)
    return raw == "true" || raw == "false"
}

/** 判断表达式是否可作为注解实参中的字面量形态。 */
internal fun CfirExpression.isAnnotationLiteralLike(): Boolean = when (this) {
    is CfirLiteralExpression -> true
    is CfirArrayLiteral -> elements.all { it.isAnnotationLiteralLike() }
    else -> false
}

/** 将字面量表达式转换为字符串；非字面量返回空。 */
internal fun CfirExpression.literalStringOrNull(): String? =
    (this as? CfirLiteralExpression)?.value?.toString()

/** 读取指定布尔命名实参的值。 */
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

/** 判断注解是否匹配指定短名。 */
private fun CfirAnnotation.matchesAnnotationName(annotationName: Name): Boolean {
    return shortNameOrNull() == annotationName
}

/** 返回注解方括号参数区间内的原始文本。 */
private fun CfirAnnotationCall.rawArgumentText(): String =
    source?.text?.toString()
        ?.substringAfter('[', missingDelimiterValue = "")
        ?.substringBeforeLast(']', missingDelimiterValue = "")
        .orEmpty()

/** 将注解原始参数文本拆分为单个实参文本。 */
private fun CfirAnnotationCall.rawArgumentTexts(): List<String> =
    rawArgumentText()
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
