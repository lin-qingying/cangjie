package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol

/**
 * 若当前 [CaType] 表示一个唯一可解析的 class-like 类型,返回对应的 [CaClassLikeSymbol];
 * 否则(例如基本类型、函数类型、元组类型、type parameter、错误类型等没有唯一 class symbol 的形态)返回 `null`。
 *
 * 对齐 Kotlin Analysis API 中 `KaType.symbol` 顶层属性,提供快捷访问入口,避免使用方反复进行类型判定。
 */
val CaType.symbol: CaClassLikeSymbol?
    get() = (this as? CaClassLikeType)?.symbol
