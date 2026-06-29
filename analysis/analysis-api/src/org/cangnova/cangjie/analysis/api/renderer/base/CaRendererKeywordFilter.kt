package org.cangnova.cangjie.analysis.api.renderer.base

import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.lexer.CjKeywordToken

/**
 * 关键字过滤策略。
 *
 * - 决定一个关键字在给定上下文中是否需要写入输出;
 * - 通过 [and] / [or] 组合多个过滤条件;
 * - 提供 [ALL] / [NONE] / [onlyWith] / [without] 等工厂方法快速构造常用策略。
 *
 * 对齐 Kotlin Analysis API 的 `KaRendererKeywordFilter`。
 */
fun interface CaRendererKeywordFilter {
    /** 返回 true 表示当前关键字应当渲染。 */
    fun filter(analysisSession: CaSession, modifier: CjKeywordToken, annotated: CaAnnotated): Boolean

    /** 与另一过滤器做逻辑与, 两者都通过才输出。 */
    infix fun and(other: CaRendererKeywordFilter): CaRendererKeywordFilter {
        val self = this
        return CaRendererKeywordFilter filter@{ modifier, kaAnnotated ->
            val analysisSession = this@filter
            self.filter(analysisSession, modifier, kaAnnotated) && other.filter(analysisSession, modifier, kaAnnotated)
        }
    }

    /** 与另一过滤器做逻辑或, 任一通过即输出。 */
    infix fun or(other: CaRendererKeywordFilter): CaRendererKeywordFilter {
        val self = this
        return CaRendererKeywordFilter filter@{ modifier, symbol ->
            val analysisSession = this@filter
            self.filter(analysisSession, modifier, symbol) || other.filter(analysisSession, modifier, symbol)
        }
    }

    /** 预设: 放行所有关键字。 */
    object ALL : CaRendererKeywordFilter {
        /**
         * 始终允许关键字渲染。
         */
        override fun filter(analysisSession: CaSession, modifier: CjKeywordToken, annotated: CaAnnotated): Boolean {
            return true
        }
    }

    /** 预设: 拒绝所有关键字。 */
    object NONE : CaRendererKeywordFilter {
        /**
         * 始终拒绝关键字渲染。
         */
        override fun filter(analysisSession: CaSession, modifier: CjKeywordToken, annotated: CaAnnotated): Boolean {
            return false
        }
    }

    companion object {
        /** 通过谓词构建过滤器, 谓词在 [CaSession] 上下文中执行。 */
        operator fun invoke(
            predicate: CaSession.(modifier: CjKeywordToken, annotated: CaAnnotated) -> Boolean
        ): CaRendererKeywordFilter =
            object : CaRendererKeywordFilter {
                /**
                 * 委托调用方提供的 session 谓词执行过滤。
                 */
                override fun filter(
                    analysisSession: CaSession,
                    modifier: CjKeywordToken,
                    annotated: CaAnnotated
                ): Boolean {
                    return predicate(analysisSession, modifier, annotated)
                }
            }

        /** 仅允许给定的关键字集合(可变参数版本)。 */
        fun onlyWith(vararg modifiers: CjKeywordToken): CaRendererKeywordFilter =
            CaRendererKeywordFilter { modifier, _ -> modifier in modifiers }

        /** 仅允许给定的关键字集合(TokenSet 版本)。 */
        fun onlyWith(modifiers: TokenSet): CaRendererKeywordFilter =
            CaRendererKeywordFilter { modifier, _ -> modifier in modifiers }

        /** 排除给定的关键字集合(可变参数版本)。 */
        fun without(vararg modifiers: CjKeywordToken): CaRendererKeywordFilter =
            CaRendererKeywordFilter { modifier, _ -> modifier !in modifiers }

        /** 排除给定的关键字集合(TokenSet 版本)。 */
        fun without(modifiers: TokenSet): CaRendererKeywordFilter =
            CaRendererKeywordFilter { modifier, _ -> modifier !in modifiers }
    }
}
