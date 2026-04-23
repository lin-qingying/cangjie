package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.CaNonPublicApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.findCDoc
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjNonPublicApi

/**
 * 从 light declaration 恢复文档文本。
 *
 * 该入口不引入新的文档组件，而是直接基于结构化 CDoc 渲染：
 * 1. 先通过 `origin.sourceElement` 恢复真实声明 PSI；
 * 2. 再走 `findCDoc()` 主线恢复结构化 descriptor；
 * 3. 最后在 light declaration 边界内把 descriptor 渲染为文本。
 *
 * 这样 source-backed、library source-backed 与 decompiled light declaration
 * 都共享同一条恢复链路；若当前 light declaration 不存在真实声明 PSI，
 * 或者对应声明本身没有文档，则返回 `null`。
 */
@OptIn(CaNonPublicApi::class, CjNonPublicApi::class)
fun CaSession.documentation(lightDeclaration: CaLightDeclaration): String? {
    val declaration = lightDeclaration.origin.sourceElement as? CjDeclaration ?: return null
    return with(this) {
        declaration.symbol.findCDoc()?.renderToDocumentationString()
    }
}

@OptIn(CjNonPublicApi::class)
private fun CDocCommentDescriptor.renderToDocumentationString(): String? {
    val rendered = buildString {
        val primaryContent = primaryTag.getContent().trim()
        if (primaryContent.isNotEmpty()) {
            append(primaryContent)
        }

        additionalSections
            .filterNot { section -> section == primaryTag }
            .forEach { section ->
                val renderedSection = section.renderSectionLine()
                if (renderedSection.isNotEmpty()) {
                    if (isNotEmpty()) appendLine()
                    append(renderedSection)
                }
            }
    }

    return rendered.ifBlank { null }
}

@OptIn(CjNonPublicApi::class)
private fun CDocSection.renderSectionLine(): String {
    val tagName = name ?: return ""
    val content = getContent().trim()
    val subjectName = getSubjectName()

    return buildString {
        append("@")
        append(tagName)
        if (!subjectName.isNullOrBlank()) {
            append(" ")
            append(subjectName)
        }
        if (content.isNotEmpty()) {
            append(" ")
            append(content)
        }
    }
}
