package org.cangnova.cangjie.analysis.api.renderer.base

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.lexer.CjKeywordToken

fun interface CaKeywordRenderer {
    fun renderKeyword(
        analysisSession: CaSession,
        keyword: CjKeywordToken,
        owner: CaAnnotated,
        keywordsRenderer: CaKeywordsRenderer,
        printer: PrettyPrinter,
    )

    fun renderKeywords(
        analysisSession: CaSession,
        keywords: List<CjKeywordToken>,
        owner: CaAnnotated,
        keywordsRenderer: CaKeywordsRenderer,
        printer: PrettyPrinter,
    ) {
        val applicableKeywords = keywords.filter { keywordsRenderer.keywordFilter.filter(analysisSession, it, owner) }
        printer.printCollection(applicableKeywords, separator = " ") {
            renderKeyword(analysisSession, it, owner, keywordsRenderer, this)
        }
    }

    object AS_WORD : CaKeywordRenderer {
        override fun renderKeyword(
            analysisSession: CaSession,
            keyword: CjKeywordToken,
            owner: CaAnnotated,
            keywordsRenderer: CaKeywordsRenderer,
            printer: PrettyPrinter,
        ) {
            if (keywordsRenderer.keywordFilter.filter(analysisSession, keyword, owner)) {
                printer.append(keyword.value)
            }
        }
    }

    object NONE : CaKeywordRenderer {
        override fun renderKeyword(
            analysisSession: CaSession,
            keyword: CjKeywordToken,
            owner: CaAnnotated,
            keywordsRenderer: CaKeywordsRenderer,
            printer: PrettyPrinter,
        ) {
        }
    }
}
