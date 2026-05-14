package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjFile

/**
 * 库源码定位协议。
 *
 * 设计要点/职责:
 * - 为来源于已编译库(.cjo / 包归档)的声明提供源文件层级的回溯入口,IDE 用于跳转到源/反编译展示。
 * - 当前接口暂作能力插槽预留,后续扩展按需补全 API,避免在未稳定前暴露细节实现。
 *
 * 对齐 Kotlin Analysis API 的 `KaSourceProvider`。
 */
interface CaSourceProvider : CaSessionComponent