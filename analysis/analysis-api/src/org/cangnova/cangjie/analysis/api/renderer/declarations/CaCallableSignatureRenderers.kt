package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol

/**
 * 把 callable 符号渲染成可写入输出的"展示名"。
 *
 * 该函数集中处理那些没有真正源码名字的 callable:
 * - 构造器渲染为 `init`
 * - finalizer 渲染为 `~init`
 * - 普通命名符号取其源码名
 * - 其他匿名 callable 显示为占位符 `<anonymous>`
 *
 * 内部使用, 仅供本目录下其他 renderer 复用。
 */
internal fun CaCallableSymbol.renderNameText(): String = when (this) {
    is CaConstructorSymbol -> "init"
    is CaFinalizerSymbol -> "~init"
    is CaNamedSymbol -> name.asString()
    else -> "<anonymous>"
}
