package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 按当前 package、可选所属类型和声明名组合声明全限定名。
 *
 * 顶层声明落在 package 下，成员声明落在 [owningClassFqName] 下；根包声明会使用 top-level 名称。
 */
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

/**
 * 去除 CFIR 可读渲染器附加的包裹标记，并规整反编译 stub 使用的类型文本。
 */
internal fun normalizeRenderedTypeText(rendered: String): String {
    return rendered.removePrefix("R|").removeSuffix("|").trim()
}

/**
 * 从完整类型文本中提取适合用作 stub 简名的短类型名。
 */
internal fun extractShortTypeName(typeText: String): String {
    return typeText
        .substringBefore('<')
        .substringBefore('?')
        .substringAfterLast('.')
        .ifBlank { "Extend" }
}

/**
 * 将任意名称文本规范化为 PSI stub 可接受的简单标识符片段。
 */
internal fun sanitizeStubSimpleName(name: String): String {
    val sanitized = name.replace(Regex("[^A-Za-z0-9_]"), "_").trim('_')
    return sanitized.ifBlank { "Extend" }
}
