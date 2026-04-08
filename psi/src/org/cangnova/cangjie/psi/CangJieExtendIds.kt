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
    val normalizedReceiverType = receiverTypeText?.trim()?.takeUnless { it.isBlank() } ?: "Unknown"
    val normalizedSuperTypes = superTypeTexts
        .map(String::trim)
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
