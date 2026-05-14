package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.psi.CjDeclarationWithBody
import org.cangnova.cangjie.psi.CjDeclarationWithInitializer
import org.cangnova.cangjie.psi.CjExpression

/**
 * 函数体 renderer。
 *
 * Kotlin 只提供 `NO_BODY`，仓颉在同一 slot 上补充 source / placeholder
 * 两种明细渲染能力，仍由 preset 组合决定是否启用。
 *
 * 对齐 Kotlin Analysis API 的 `KaFunctionLikeBodyRenderer`。
 */
fun interface CaFunctionLikeBodyRenderer {
    /** 渲染 [symbol] 的函数体到 [printer]。 */
    fun renderBody(
        analysisSession: CaSession,
        symbol: CaFunctionSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /** 预设: 不输出函数体, 仅渲染签名。 */
        val NO_BODY: CaFunctionLikeBodyRenderer = CaFunctionLikeBodyRenderer { _, _, _, _ -> }

        /**
         * 仅对 source PSI 可恢复的函数输出真实 body / expression body。
         */
        val AS_SOURCE: CaFunctionLikeBodyRenderer = CaFunctionLikeBodyRenderer { _, symbol, _, printer ->
            val declaration = symbol.psi as? CjDeclarationWithBody ?: return@CaFunctionLikeBodyRenderer
            renderCallableSourceBody(
                printer,
                declaration.bodyExpression,
                declaration.hasBlockBody(),
                declaration as? CjDeclarationWithInitializer,
            )
        }

        /**
         * 仅保留“存在函数体”的结构，不暴露源码细节。
         */
        val AS_PLACEHOLDER: CaFunctionLikeBodyRenderer = CaFunctionLikeBodyRenderer { _, symbol, _, printer ->
            val declaration = symbol.psi as? CjDeclarationWithBody ?: return@CaFunctionLikeBodyRenderer
            renderCallablePlaceholderBody(
                printer,
                declaration.bodyExpression,
                declaration.hasBlockBody(),
                declaration as? CjDeclarationWithInitializer,
            )
        }
    }
}

/**
 * 写出 callable 的源码 body。
 *
 * - 含块状 body: 直接输出 `{ ... }` 文本;
 * - 有初始化器或表达式 body: 输出 `= xxx` 形式。
 */
internal fun renderCallableSourceBody(
    printer: PrettyPrinter,
    bodyExpression: CjExpression?,
    hasBlockBody: Boolean,
    initializerOwner: CjDeclarationWithInitializer?,
) {
    when {
        bodyExpression != null && hasBlockBody -> printer.withPrefix(" ") {
            printer.append(bodyExpression.text)
        }

        initializerOwner?.initializer != null -> printer.append(" = ${initializerOwner.initializer!!.text}")
        bodyExpression != null -> printer.append(" = ${bodyExpression.text}")
    }
}

/**
 * 写出 callable 的 placeholder body, 用 `...` 屏蔽具体细节。
 *
 * - 块状 body 输出 `{ ... }`;
 * - 表达式 body / 初始化器输出 `= ...`。
 */
internal fun renderCallablePlaceholderBody(
    printer: PrettyPrinter,
    bodyExpression: CjExpression?,
    hasBlockBody: Boolean,
    initializerOwner: CjDeclarationWithInitializer?,
) {
    when {
        bodyExpression != null && hasBlockBody -> printer.append(" { ... }")
        initializerOwner?.initializer != null || bodyExpression != null -> printer.append(" = ...")
    }
}
