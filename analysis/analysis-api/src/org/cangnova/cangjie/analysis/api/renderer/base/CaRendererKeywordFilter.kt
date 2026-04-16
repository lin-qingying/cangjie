package org.cangnova.cangjie.analysis.api.renderer.base

import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.lexer.CjKeywordToken

fun interface CaRendererKeywordFilter {
    fun filter(analysisSession: CaSession, modifier: CjKeywordToken, annotated: CaAnnotated): Boolean

    infix fun and(other: CaRendererKeywordFilter): CaRendererKeywordFilter {
        val self = this
        return CaRendererKeywordFilter filter@{ modifier, kaAnnotated ->
            val analysisSession = this@filter
            self.filter(analysisSession, modifier, kaAnnotated) && other.filter(analysisSession, modifier, kaAnnotated)
        }
    }

    infix fun or(other: CaRendererKeywordFilter): CaRendererKeywordFilter {
        val self = this
        return CaRendererKeywordFilter filter@{ modifier, symbol ->
            val analysisSession = this@filter
            self.filter(analysisSession, modifier, symbol) || other.filter(analysisSession, modifier, symbol)
        }
    }

    object ALL : CaRendererKeywordFilter {
        override fun filter(analysisSession: CaSession, modifier: CjKeywordToken, annotated: CaAnnotated): Boolean {
            return true
        }
    }

    object NONE : CaRendererKeywordFilter {
        override fun filter(analysisSession: CaSession, modifier: CjKeywordToken, annotated: CaAnnotated): Boolean {
            return false
        }
    }

    companion object {
        operator fun invoke(
            predicate: CaSession.(modifier: CjKeywordToken, annotated: CaAnnotated) -> Boolean
        ): CaRendererKeywordFilter =
            object : CaRendererKeywordFilter {
                override fun filter(
                    analysisSession: CaSession,
                    modifier: CjKeywordToken,
                    annotated: CaAnnotated
                ): Boolean {
                    return predicate(analysisSession, modifier, annotated)
                }
            }

        fun onlyWith(vararg modifiers: CjKeywordToken): CaRendererKeywordFilter =
            CaRendererKeywordFilter { modifier, _ -> modifier in modifiers }

        fun onlyWith(modifiers: TokenSet): CaRendererKeywordFilter =
            CaRendererKeywordFilter { modifier, _ -> modifier in modifiers }

        fun without(vararg modifiers: CjKeywordToken): CaRendererKeywordFilter =
            CaRendererKeywordFilter { modifier, _ -> modifier !in modifiers }

        fun without(modifiers: TokenSet): CaRendererKeywordFilter =
            CaRendererKeywordFilter { modifier, _ -> modifier !in modifiers }
    }
}
