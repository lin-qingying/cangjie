package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol

/**
 * callable 接收者(receiver) renderer。
 *
 * 决定扩展函数/属性等的接收者类型前缀如何写出, 例如 `MyClass.foo()`
 * 中的 `MyClass.` 部分。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallableReceiverRenderer`。
 */
fun interface CaCallableReceiverRenderer {
    /** 写出 [symbol] 的接收者前缀到 [printer]。 */
    fun renderReceiver(
        analysisSession: CaSession,
        symbol: CaCallableSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )
}
