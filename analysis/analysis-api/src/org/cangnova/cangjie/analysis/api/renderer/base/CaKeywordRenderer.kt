package org.cangnova.cangjie.analysis.api.renderer.base

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.lexer.CjKeywordToken

/**
 * 关键字渲染策略入口。
 *
 * - 决定关键字(`public`、`private`、`override` 等)在渲染输出中以何种字面形式出现;
 * - 与 [CaKeywordsRenderer] 配合: 后者负责持有此策略与过滤器, 本接口负责具体写出;
 * - 通过 [AS_WORD] / [NONE] 等预设 object 直接复用常见配置。
 *
 * 对齐 Kotlin Analysis API 的 `KaKeywordRenderer`。
 */
fun interface CaKeywordRenderer {
    /**
     * 渲染单个关键字到 [printer]。
     *
     * 实现需要先经由 [CaKeywordsRenderer.keywordFilter] 判定是否输出。
     */
    fun renderKeyword(
        analysisSession: CaSession,
        keyword: CjKeywordToken,
        owner: CaAnnotated,
        keywordsRenderer: CaKeywordsRenderer,
        printer: PrettyPrinter,
    )

    /**
     * 渲染一组关键字, 默认实现使用空格分隔。
     *
     * 不在过滤器中的关键字会被跳过, 仅保留可见关键字。
     */
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

    /**
     * 将关键字按字面值直接输出, 例如 `public` 写为 `"public"`。
     *
     * 适合源码风格渲染。
     */
    object AS_WORD : CaKeywordRenderer {
        /**
         * 当过滤器允许时按关键字 token 的字面值输出。
         */
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

    /**
     * 不渲染任何关键字, 输出始终为空。
     *
     * 用于需要彻底屏蔽关键字的场景(如仅输出签名核心)。
     */
    object NONE : CaKeywordRenderer {
        /**
         * 丢弃关键字，不向 printer 写入内容。
         */
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
