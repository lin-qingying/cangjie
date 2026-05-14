package org.cangnova.cangjie.analysis.api.permissions

/**
 * 临时允许在 EDT 上执行 [action]。
 *
 * 通过修改 [CaAnalysisPermissionRegistry] 的 EDT 允许标志,
 * 在 [action] 运行期间放开默认禁止;函数返回前会恢复原值。
 * 若已经处于允许状态则直接执行,不做嵌套切换。
 *
 * 仅在调用上下文已被严格限速的场景使用,避免阻塞 UI 线程。
 */
inline fun <T> allowAnalysisOnEdt(action: () -> T): T {
    val permissionRegistry = CaAnalysisPermissionRegistry.getInstance()
    if (permissionRegistry.isAnalysisAllowedOnEdt) return action()

    permissionRegistry.isAnalysisAllowedOnEdt = true
    try {
        return action()
    } finally {
        permissionRegistry.isAnalysisAllowedOnEdt = false
    }
}

/**
 * 在 [action] 范围内显式禁止执行 analyze 操作。
 *
 * 用于声明"这里绝不应触发分析"的临界区(例如序列化、PSI 改写中),
 * [description] 会在违规调用时附在异常里,帮助定位入口。
 * 若外层已设置过禁止条件,直接复用原值,不嵌套覆盖。
 */
inline fun <R> forbidAnalysis(description: String, action: () -> R): R {
    val permissionRegistry = CaAnalysisPermissionRegistry.getInstance()
    if (permissionRegistry.explicitAnalysisRestriction != null) return action()

    permissionRegistry.explicitAnalysisRestriction =
        CaAnalysisPermissionRegistry.CaExplicitAnalysisRestriction(description)
    return try {
        action()
    } finally {
        permissionRegistry.explicitAnalysisRestriction = null
    }
}

/**
 * 临时允许在 write action 中执行 [action]。
 *
 * 默认禁止在 write 中分析,以避免读写交错导致 IDE 卡顿或解析状态不一致;
 * 仅当上下文已确认安全(如缓存命中、不会触发深度 resolve)时启用。
 * 嵌套调用时不再重复切换。
 */
inline fun <T> allowAnalysisFromWriteAction(action: () -> T): T {
    val permissionRegistry = CaAnalysisPermissionRegistry.getInstance()
    if (permissionRegistry.isAnalysisAllowedInWriteAction) return action()

    permissionRegistry.isAnalysisAllowedInWriteAction = true
    try {
        return action()
    } finally {
        permissionRegistry.isAnalysisAllowedInWriteAction = false
    }
}
