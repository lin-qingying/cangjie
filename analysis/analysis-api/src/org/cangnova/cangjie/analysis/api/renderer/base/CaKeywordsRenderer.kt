package org.cangnova.cangjie.analysis.api.renderer.base

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.lexer.CjKeywordToken

/**
 * 关键字渲染器。
 */
class CaKeywordsRenderer(
    val keywordRenderer: CaKeywordRenderer,
    val keywordFilter: CaRendererKeywordFilter,
) {

    fun renderKeywords(
        analysisSession: CaSession,
        keywords: List<CjKeywordToken>,
        owner: CaAnnotated,
        printer: PrettyPrinter
    ) {
        keywordRenderer.renderKeywords(analysisSession, keywords, owner, this, printer)
    }

    fun renderKeyword(
        analysisSession: CaSession,
        keyword: CjKeywordToken,
        owner: CaAnnotated,
        printer: PrettyPrinter
    ) {
        keywordRenderer.renderKeyword(analysisSession, keyword, owner, this, printer)
    }

    inline fun with(action: Builder.() -> Unit): CaKeywordsRenderer {
        val renderer = this
        return CaKeywordsRenderer {
            this.keywordRenderer = renderer.keywordRenderer
            this.keywordFilter = renderer.keywordFilter
            action()
        }
    }

    class Builder {
        lateinit var keywordRenderer: CaKeywordRenderer
        lateinit var keywordFilter: CaRendererKeywordFilter

        fun build(): CaKeywordsRenderer = CaKeywordsRenderer(
            keywordRenderer,
            keywordFilter
        )
    }

    companion object {
        val AS_WORD: CaKeywordsRenderer =
            CaKeywordsRenderer(CaKeywordRenderer.AS_WORD, CaRendererKeywordFilter.ALL)
        val NONE: CaKeywordsRenderer = CaKeywordsRenderer(CaKeywordRenderer.NONE, CaRendererKeywordFilter.ALL)
        inline operator fun invoke(action: Builder.() -> Unit): CaKeywordsRenderer =
            Builder().apply(action).build()
    }
}
