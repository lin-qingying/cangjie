package org.cangnova.cangjie.analysis.api.renderer.base

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.lexer.CjKeywordToken

/**
 * 关键字渲染器聚合配置。
 *
 * - 把"如何写出关键字字面值"([CaKeywordRenderer])与"是否输出此关键字"([CaRendererKeywordFilter])组合在一起;
 * - 提供 [renderKeyword] / [renderKeywords] 高层入口, 内部委托给 [keywordRenderer];
 * - 通过 [AS_WORD] / [NONE] 等预设直接复用常见组合, 或使用 [Builder] 自定义。
 *
 * 对齐 Kotlin Analysis API 的 `KaKeywordsRenderer`。
 */
class CaKeywordsRenderer(
    /** 负责具体写出关键字文本的 renderer。 */
    val keywordRenderer: CaKeywordRenderer,

    /** 负责判断关键字是否应被输出的过滤器。 */
    val keywordFilter: CaRendererKeywordFilter,
) {

    /** 渲染一组关键字, 不在过滤器内的关键字会被跳过。 */
    fun renderKeywords(
        analysisSession: CaSession,
        keywords: List<CjKeywordToken>,
        owner: CaAnnotated,
        printer: PrettyPrinter
    ) {
        keywordRenderer.renderKeywords(analysisSession, keywords, owner, this, printer)
    }

    /** 渲染单个关键字, 委托给 [keywordRenderer]。 */
    fun renderKeyword(
        analysisSession: CaSession,
        keyword: CjKeywordToken,
        owner: CaAnnotated,
        printer: PrettyPrinter
    ) {
        keywordRenderer.renderKeyword(analysisSession, keyword, owner, this, printer)
    }

    /**
     * 基于当前配置派生一个新的渲染器。
     *
     * 在 [action] 中可以重新指定 [Builder.keywordRenderer] 或 [Builder.keywordFilter],
     * 未显式覆盖的字段沿用原渲染器。
     */
    inline fun with(action: Builder.() -> Unit): CaKeywordsRenderer {
        val renderer = this
        return CaKeywordsRenderer {
            this.keywordRenderer = renderer.keywordRenderer
            this.keywordFilter = renderer.keywordFilter
            action()
        }
    }

    /**
     * 渲染器构建器, 用于以 DSL 方式装配 [CaKeywordsRenderer]。
     */
    class Builder {
        /** 关键字字面渲染策略, 必须在构建前赋值。 */
        lateinit var keywordRenderer: CaKeywordRenderer

        /** 关键字过滤策略, 必须在构建前赋值。 */
        lateinit var keywordFilter: CaRendererKeywordFilter

        /** 构建最终渲染器。 */
        fun build(): CaKeywordsRenderer = CaKeywordsRenderer(
            keywordRenderer,
            keywordFilter
        )
    }

    companion object {
        /** 预设: 关键字按字面值输出, 并接收所有过滤项。 */
        val AS_WORD: CaKeywordsRenderer =
            CaKeywordsRenderer(CaKeywordRenderer.AS_WORD, CaRendererKeywordFilter.ALL)

        /** 预设: 不输出任何关键字, 适合纯类型签名场景。 */
        val NONE: CaKeywordsRenderer = CaKeywordsRenderer(CaKeywordRenderer.NONE, CaRendererKeywordFilter.ALL)

        /** DSL 入口, 等价于 `Builder().apply(action).build()`。 */
        inline operator fun invoke(action: Builder.() -> Unit): CaKeywordsRenderer =
            Builder().apply(action).build()
    }
}
