package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * symbol 元信息协议。
 *
 * 设计要点/职责:
 * - `createPointer()` 已经由 `CaSymbol` 成员协议直接承担,
 *   这里保留为 symbol 元信息扩展的稳定插槽,
 *   避免继续维护与成员 API 重复的一套 component 入口。
 * - 后续若新增不便落在 symbol 自身的元信息查询(例如统计、来源标注),
 *   都应统一聚集到该协议下。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbolInformationProvider`。
 */
interface CaSymbolInformationProvider : CaLifetimeOwner
