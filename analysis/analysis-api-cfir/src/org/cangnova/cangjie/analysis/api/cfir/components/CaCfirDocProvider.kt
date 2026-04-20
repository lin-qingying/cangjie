package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirBackedSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirDeclarationBackedSymbol
import org.cangnova.cangjie.analysis.api.components.CaDocProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.psi.CjDeclaration

/**
 * 文档提供组件。
 *
 * 对齐 Kotlin 的 `KaFirKDocProvider` 落位，不再额外保留 `DocumentationProtocol`。
 * 文档恢复逻辑由组件文件内的私有 helper 承载，并统一走 session 缓存。
 */
internal class CaCfirDocProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDocProvider {
    override fun CaSymbol.documentation(): String? = withValidityAssertion {
        analysisSession.renderDocumentation(this@documentation)
    }
}

/**
 * 统一的公开符号文档提取入口。
 *
 * `analysis-api-cfir` 不再把文档读取散落在各个 provider 中，
 * 而是统一复用 session 级缓存、source/original/navigation 恢复链路，
 * 让 source-backed、light declaration、decompiled 边界都走同一条主线。
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
 * 统一把公开 symbol 恢复成真正承载 CDoc 的声明 PSI。
 */
private fun CaCfirSession.findDocumentedDeclaration(symbol: CaSymbol): CjDeclaration? {
    val sourcePsi = when (symbol) {
        is CaCfirDeclarationBackedSymbol<*> -> symbol.psi
        is CaCfirBackedSymbol<*> -> symbolQueries.lookupSourcePsi(symbol.backingSymbol)
        else -> null
    } ?: return null

    return sourcePsi.asDocumentedDeclaration()
}

/**
 * 从 source / original / navigation 三种视图中恢复真实声明。
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
 * 将 CDoc 规范化成稳定的公开文档文本。
 *
 * 这里优先保证：
 * 1. 标签顺序不丢失；
 * 2. `@return` / `@see` 这类非 section-start 标签不丢失；
 * 3. `[Subject]` 形式的 subject link 在公开文本里直接可读。
 */
private fun CDoc.renderDocumentationText(): String {
    return text.lineSequence()
        .map(::normalizeDocumentationLine)
        .filter(String::isNotBlank)
        .joinToString("\n")
        .trim()
}

private fun normalizeDocumentationLine(line: String): String {
    return line.trim()
        .removePrefix("/**")
        .removeSuffix("*/")
        .trim()
        .removePrefix("*")
        .trim()
        .replace(subjectLinkRegex, "$1")
}

private val subjectLinkRegex = Regex("""\[(.+?)]""")
