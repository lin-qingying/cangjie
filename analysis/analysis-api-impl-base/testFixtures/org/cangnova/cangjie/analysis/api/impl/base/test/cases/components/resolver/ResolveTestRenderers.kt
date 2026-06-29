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
/**
 * 渲染 resolver 测试中的公开 symbol 摘要。
 *
 * callable symbol 会额外输出 callableId、receiverType 和 returnType。
 */
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
/**
 * 渲染 resolver 测试中的公开类型摘要。
 *
 * `null` 类型显式渲染为字符串 `null`。
 */
internal fun renderTypeForResolveTest(type: CaType?): String {
    return type.renderForResolveTest()
}

context(session: CaSession)
/**
 * 使用 qualified type renderer 渲染可空类型。
 *
 * 渲染结果会把内部 `/` 分隔符规范化为 `.`。
 */
private fun CaType?.renderForResolveTest(): String {
    val type = this ?: return "null"
    return with(session) {
        type.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)
    }.replace('/', '.')
}

/**
 * 删除 `StringBuilder` 末尾的单个换行符。
 *
 * resolver 输出器用它避免非 callable symbol 的尾部多出空行。
 */
private fun StringBuilder.deleteSuffixLineBreak() {
    if (endsWith("\n")) {
        deleteCharAt(length - 1)
    }
}
