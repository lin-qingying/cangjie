package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * resolver 测试统一输出器。
 *
 * 这里只渲染当前仓颉公开 API 已经稳定暴露的信息：
 * 1. symbol 运行时类型；
 * 2. 可命名符号名称；
 * 3. callableId / receiver / returnType。
 *
 * 不额外暴露尚未进入公开 API 的候选、底层约束或后端内部细节。
 */
context(session: CaSession)
internal fun renderSymbolForResolveTest(symbol: CaSymbol?): String {
    if (symbol == null) return "null"

    return buildString {
        appendLine("symbolClass: ${symbol::class.simpleName}")
        symbol.name?.asString()?.let { appendLine("name: $it") }

        if (symbol is CaCallableSymbol) {
            appendLine("callableId: ${symbol.callableId?.asSingleFqName()?.asString() ?: "null"}")
            appendLine("receiverType: ${symbol.receiverType.renderForResolveTest()}")
            append("returnType: ${symbol.returnType.renderForResolveTest()}")
        } else {
            deleteSuffixLineBreak()
        }
    }
}

context(session: CaSession)
internal fun renderTypeForResolveTest(type: CaType?): String {
    return type.renderForResolveTest()
}

context(session: CaSession)
private fun CaType?.renderForResolveTest(): String {
    val type = this ?: return "null"
    return with(session) {
        type.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)
    }.replace('/', '.')
}

private fun StringBuilder.deleteSuffixLineBreak() {
    if (endsWith("\n")) {
        deleteCharAt(length - 1)
    }
}
