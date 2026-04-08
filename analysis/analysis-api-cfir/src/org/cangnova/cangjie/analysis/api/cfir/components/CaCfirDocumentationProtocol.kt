package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.psi.CjDeclaration

/**
 * 统一的公开符号文档提取协议。
 *
 * `analysis-api-cfir` 不再把文档渲染散落在各个 provider 中，
 * 而是统一复用 session 级缓存、源码导航和公开符号恢复协议。
 */
internal fun CaCfirSession.renderDocumentation(symbol: CaSymbol): String? {
    val documentationKey = symbol.publicSymbolCacheKeyOrNull() ?: return null

    return getOrCreateDocumentation(documentationKey) {
        findDocumentedDeclaration(symbol)
            ?.docComment
            ?.renderDocumentationText()
    }
}

/**
 * 统一把公开 symbol 恢复为“真正承载 CDoc 的声明 PSI”。
 *
 * 这里不把文档读取绑定到某一种 symbol 实现细节上，而是按以下顺序恢复：
 * 1. 声明背后的 source PSI；
 * 2. 原始元素 / 导航元素；
 * 3. 仍然找不到时返回 null。
 *
 * 这样后续 light declaration、decompiled source view 或其他 symbol 入口
 * 接入文档能力时，不需要再重新发明一套恢复协议。
 */
private fun CaCfirSession.findDocumentedDeclaration(symbol: CaSymbol): CjDeclaration? {
    val sourcePsi = when (symbol) {
        is CaCfirDeclarationBackedSymbol<*> -> symbol.psi
        is CaCfirBackedSymbol<*> -> lookupSourcePsi(symbol.backingSymbol)
        else -> null
    } ?: return null

    return sourcePsi.asDocumentedDeclaration()
}

/**
 * 从 source/original/navigation 三种视图中恢复真正的声明节点。
 */
private fun Any?.asDocumentedDeclaration(): CjDeclaration? {
    val element = this as? PsiElement ?: return null
    return sequenceOf(
        element,
        element.originalElement,
        element.navigationElement,
    )
        .filterIsInstance<CjDeclaration>()
        .firstOrNull()
}

/**
 * 将 `CDoc` 结构渲染成稳定的公开文档文本。
 *
 * 默认段落保留原始正文；具名段落按 `@tag [subject] content`
 * 的顺序追加，这样 Analysis API 返回的是稳定且贴近源码的文档视图。
 */
private fun CDoc.renderDocumentationText(): String {
    val sections = mutableListOf<String>()

    val defaultContent = getDefaultSection().getContent().trim()
    if (defaultContent.isNotEmpty()) {
        sections += defaultContent
    }

    getAllSections()
        .filterNot { it == getDefaultSection() }
        .forEach { section ->
            val sectionName = section.name ?: return@forEach
            val subject = section.getSubjectName()?.takeIf(String::isNotBlank)
            val content = section.getContent().trim()

            val header = buildString {
                append('@')
                append(sectionName)
                if (subject != null) {
                    append(' ')
                    append(subject)
                }
            }

            sections += if (content.isEmpty()) header else "$header $content"
        }

    return sections.joinToString(separator = "\n").trim().ifEmpty { text.trim() }
}
