package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.CaNonPublicApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.findCDoc
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjNonPublicApi
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType

/**
 * 从 light declaration 恢复其文档文本。
 *
 * 本入口不引入新的文档组件,而是直接基于结构化 CDoc 渲染:
 * 1. 先通过 `origin.sourceElement` 恢复真实声明 PSI;
 * 2. 再走 `findCDoc()` 主线恢复结构化 descriptor;
 * 3. 最后在 light declaration 边界内把 descriptor 渲染为文本。
 *
 * 这样 source-backed、library source-backed 与 decompiled light declaration
 * 都共享同一条恢复链路;若当前 light declaration 不存在真实声明 PSI,
 * 或者对应声明本身没有文档,则返回 `null`。
 */
@OptIn(CaNonPublicApi::class, CjNonPublicApi::class)
fun CaSession.documentation(lightDeclaration: CaLightDeclaration): String? {
    val declaration = lightDeclaration.origin.sourceElement as? CjDeclaration ?: return null
    return with(this) {
        declaration.findCDoc()?.renderToDocumentationString()
    }
}

/**
 * 将结构化 CDoc descriptor 渲染为纯文本文档串。
 *
 * 顺序:先渲染主标签内容,再按需追加可渲染的子标签行。
 * 全部为空时返回 `null`,由调用方决定是否回退到其他来源。
 */
@OptIn(CjNonPublicApi::class)
private fun CDocCommentDescriptor.renderToDocumentationString(): String? {
    val rendered = buildString {
        val primaryContent = primaryTag.getContent().trim()
        if (primaryContent.isNotEmpty()) {
            append(primaryContent)
        }

        collectRenderableTags().forEach { tag ->
            val renderedTag = tag.renderTagLine()
            if (renderedTag.isNotEmpty()) {
                if (isNotEmpty()) appendLine()
                append(renderedTag)
            }
        }
    }

    return rendered.ifBlank { null }
}

/**
 * 收集主标签与附加段落中所有"具名"的子标签,作为后续渲染输入。
 */
@OptIn(CjNonPublicApi::class)
private fun CDocCommentDescriptor.collectRenderableTags(): List<CDocTag> {
    return buildList {
        (primaryTag as? CDocSection)
            ?.getChildrenOfType<CDocTag>()
            ?.filterTo(this) { tag -> tag.name != null }

        additionalSections
            .filterNot { section -> section == primaryTag }
            .forEach { section ->
                section.getChildrenOfType<CDocTag>()
                    .filterTo(this) { tag -> tag.name != null }
            }
    }
}

/**
 * 将单个 CDoc 标签渲染为 `@name [subject] [content]` 的形式。
 *
 * 缺少 name 时返回空串,由调用方过滤掉。
 */
@OptIn(CjNonPublicApi::class)
private fun CDocTag.renderTagLine(): String {
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
