package org.cangnova.cangjie.analysis.api

import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.psi.CjElement
import kotlin.jvm.JvmName

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
 * analyze(element) { // 当前接收者：CaSession
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

/**
 * 按源码元素批量执行分析。
 *
 * 该入口与单元素 `analyze(element)` 共享同一生命周期语义，
 * 但允许 `CaSessionProvider` 按 session 粒度分组复用分析域，
 * 避免调用方手工循环进入多个 `analyze {}` 块。
 */
@JvmName("analyzeElements")
fun <R> analyze(
    useSiteElements: Collection<CjElement>,
    action: CaSession.(CjElement) -> R,
): List<R> {
    if (useSiteElements.isEmpty()) return emptyList()

    val project = useSiteElements.first().project
    check(useSiteElements.all { it.project == project }) {
        "批量 Analysis API 分析要求所有 use-site elements 属于同一个 Project。"
    }

    return CaSessionProvider.getInstance(project)
        .analyzeElements(useSiteElements, action)
}

/**
 * 按 use-site 模块批量执行分析。
 *
 * 这里显式要求所有模块来自同一个 Project，
 * 以保证 session provider、平台权限和生命周期边界保持一致。
 */
@JvmName("analyzeModules")
fun <R> analyze(
    useSiteModules: Collection<CaModule>,
    action: CaSession.(CaModule) -> R,
): List<R> {
    if (useSiteModules.isEmpty()) return emptyList()

    val project = useSiteModules.first().project
    check(useSiteModules.all { it.project == project }) {
        "批量 Analysis API 分析要求所有 use-site modules 属于同一个 Project。"
    }

    return CaSessionProvider.getInstance(project)
        .analyzeModules(useSiteModules, action)
}
