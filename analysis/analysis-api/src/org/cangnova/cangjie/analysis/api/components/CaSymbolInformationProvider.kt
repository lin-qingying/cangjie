package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * symbol 元信息协议。
 *
 * `createPointer()` 已经由 `CaSymbol` 成员协议直接承担，
 * 这里保留为 symbol 元信息扩展的稳定插槽，
 * 避免继续维护与成员 API 重复的一套 component 入口。
 */
interface CaSymbolInformationProvider : CaLifetimeOwner
