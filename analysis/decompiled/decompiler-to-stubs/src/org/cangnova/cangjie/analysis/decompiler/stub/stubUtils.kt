package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

internal fun composeQualifiedName(
    packageFqName: FqName,
    owningClassFqName: FqName?,
    name: Name,
): FqName {
    return when {
        owningClassFqName != null -> FqName("${owningClassFqName.asString()}.${name.asString()}")
        packageFqName.isRoot -> FqName.topLevel(name)
        else -> packageFqName.child(name)
    }
}

internal fun normalizeRenderedTypeText(rendered: String): String {
    return rendered.removePrefix("R|").removeSuffix("|").trim()
}

internal fun extractShortTypeName(typeText: String): String {
    return typeText
        .substringBefore('<')
        .substringBefore('?')
        .substringAfterLast('.')
        .ifBlank { "Extend" }
}

internal fun sanitizeStubSimpleName(name: String): String {
    val sanitized = name.replace(Regex("[^A-Za-z0-9_]"), "_").trim('_')
    return sanitized.ifBlank { "Extend" }
}
