package org.cangnova.cangjie.analysis.api

import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.psi.CjElement

/**
 * 在 [CaSession] 上下文中执行分析操作（对齐 Kotlin 的 analyze()）。
 *
 * 从 [useSiteElement] 所属模块的视角进行分析。
 *
 * 约定：
 * - [CaSession] 及任何 lifetime owner 不能泄漏到 analyze 块外
 * - 跨 analyze() 块传递 lifetime owner 需使用 Pointer 模式
 *
 * 示例：
 * ```
 * analyze(element) { // this: CaSession
 *     val symbol = element.symbol
 *     val type = symbol.returnType
 * }
 * ```
 */
inline fun <R> analyze(
    useSiteElement: CjElement,
    action: CaSession.() -> R,
): R =
    CaSessionProvider.getInstance(useSiteElement.project)
        .analyze(useSiteElement, action)

/**
 * 按模块执行分析。
 */
inline fun <R> analyze(
    useSiteModule: CaModule,
    crossinline action: CaSession.() -> R,
): R {
    val sessionProvider = CaSessionProvider.getInstance(useSiteModule.project)
    return sessionProvider.analyze(useSiteModule, action)
}
