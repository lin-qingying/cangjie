package org.cangnova.cangjie.psi

import org.cangnova.cangjie.name.FqName

/**
 * `extend` 稳定身份生成器。
 *
 * `extend` 没有天然可复用的 `ClassId`，因此 compiled stub、decompiled PSI、
 * public symbol 与 pointer restore 必须共享同一套 ID 规则。
 */
fun buildExtendId(
    packageFqName: FqName?,
    receiverTypeText: String?,
    superTypeTexts: List<String>,
): String {
    val normalizedReceiverType = normalizeExtendTypeText(receiverTypeText)?.takeUnless { it.isBlank() } ?: "Unknown"
    val normalizedSuperTypes = superTypeTexts
        .mapNotNull(::normalizeExtendTypeText)
        .filter(String::isNotBlank)
        .sorted()
        .joinToString("&")
    return buildString {
        append(packageFqName?.asString().orEmpty())
        append(":")
        append(normalizedReceiverType)
        append("<:")
        append(normalizedSuperTypes)
    }
}

/**
 * 规范化 `extend` 身份中使用的类型文本。
 *
 * CFIR/debug renderer 会产出 `R|Type|` 片段；反编译 PSI 的错误 text range
 * 可能把多个片段串联起来。`extendId` 只允许消费第一个真实类型文本。
 */
fun normalizeExtendTypeText(typeText: String?): String? {
    val trimmed = typeText?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    val withoutDebugPrefix = trimmed.removePrefix("R|")
    val firstDebugSegment = withoutDebugPrefix.substringBefore("|R|")
    return firstDebugSegment.removeSuffix("|").trim()
}
