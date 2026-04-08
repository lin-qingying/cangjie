package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.CaAnnotationRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.CaPrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

/**
 * 声明名渲染协议。
 *
 * 这一层只负责“名字本身如何输出”，不承担关键字、修饰符、签名等高层格式决策，
 * 以便 hover、signature help、文档渲染共享同一套声明结构。
 */
fun interface CaDeclarationNameRenderer {
    fun renderName(name: String, printer: CaPrettyPrinter)

    companion object {
        val QUOTED: CaDeclarationNameRenderer = CaDeclarationNameRenderer { name, printer ->
            printer.append(name)
        }
    }
}

/**
 * renderer 代码风格协议。
 *
 * 当前先稳定控制最常用的空白策略，后续如果仓颉源码风格需要继续细化，可在此层继续扩展。
 */
interface CaRendererCodeStyle {
    val spaceAfterColon: Boolean

    val spaceAfterComma: Boolean
}

object CaRecommendedRendererCodeStyle : CaRendererCodeStyle {
    override val spaceAfterColon: Boolean = true
    override val spaceAfterComma: Boolean = true
}

/**
 * callable 返回类型过滤协议。
 *
 * 公开 renderer 不应把“是否省略某些返回类型”硬编码在具体声明渲染器里，
 * 而应通过稳定策略位集中描述。
 */
fun interface CaCallableReturnTypeFilter {
    fun shouldRenderReturnType(analysisSession: CaSession, symbol: CaCallableSymbol): Boolean

    companion object {
        val ALWAYS: CaCallableReturnTypeFilter = CaCallableReturnTypeFilter { _, _ -> true }

        val NO_UNIT_FOR_FUNCTIONS: CaCallableReturnTypeFilter = CaCallableReturnTypeFilter { analysisSession, symbol ->
            val returnType = with(analysisSession) { symbol.returnType }
            return@CaCallableReturnTypeFilter returnType.presentation != "Unit"
        }
    }
}

/**
 * 声明级修饰符渲染协议。
 *
 * 它统一承载注解、可见性、模态与仓颉声明前缀关键字的输出规则，
 * 避免各个 symbol renderer 分别拼接导致格式漂移。
 */
fun interface CaDeclarationModifiersRenderer {
    fun renderModifiers(
        analysisSession: CaSession,
        symbol: CaDeclarationSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )
}

object CaDeclarationModifiersRendererForSource {
    private fun renderByProviders(
        symbol: CaDeclarationSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
        visibilityProvider: CaRendererVisibilityModifierProvider,
        modalityProvider: CaRendererModalityModifierProvider,
        otherModifiersProvider: CaRendererOtherModifiersProvider,
    ) {
        val modifiers = buildList {
            visibilityProvider.getVisibilityModifier(symbol)?.let(::add)
            modalityProvider.getModalityModifier(symbol)?.let(::add)
            addAll(otherModifiersProvider.getOtherModifiers(symbol))
        }

        modifiers.forEach { modifier ->
            declarationRenderer.keywordsRenderer.renderKeyword(modifier, printer)
            printer.append(" ")
        }
    }

    val NO_IMPLICIT_MODIFIERS: CaDeclarationModifiersRenderer = CaDeclarationModifiersRenderer { analysisSession, symbol, declarationRenderer, printer ->
        declarationRenderer.annotationRenderer.renderAnnotations(with(analysisSession) { symbol.annotations }, printer)
        renderByProviders(
            symbol = symbol,
            declarationRenderer = declarationRenderer,
            printer = printer,
            visibilityProvider = CaRendererVisibilityModifierProvider.NO_IMPLICIT_VISIBILITY,
            modalityProvider = CaRendererModalityModifierProvider.NO_IMPLICIT_MODALITY,
            otherModifiersProvider = CaRendererOtherModifiersProvider.DECLARATION_PREFIX_ONLY,
        )
    }

    val WITH_IMPLICIT_MODIFIERS: CaDeclarationModifiersRenderer = CaDeclarationModifiersRenderer { analysisSession, symbol, declarationRenderer, printer ->
        declarationRenderer.annotationRenderer.renderAnnotations(with(analysisSession) { symbol.annotations }, printer)
        renderByProviders(
            symbol = symbol,
            declarationRenderer = declarationRenderer,
            printer = printer,
            visibilityProvider = CaRendererVisibilityModifierProvider.WITH_IMPLICIT_VISIBILITY,
            modalityProvider = CaRendererModalityModifierProvider.WITH_IMPLICIT_MODALITY,
            otherModifiersProvider = CaRendererOtherModifiersProvider.DECLARATION_PREFIX_ONLY,
        )
    }
}
