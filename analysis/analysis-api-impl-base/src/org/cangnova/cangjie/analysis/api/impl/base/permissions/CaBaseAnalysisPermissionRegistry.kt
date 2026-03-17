package org.cangnova.cangjie.analysis.api.impl.base.permissions

import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry.CaExplicitAnalysisRestriction

/**
 * 分析权限注册表的基础实现。
 *
 * 基于线程本地变量保存当前线程内的分析限制与权限开关，避免跨线程污染。
 */
internal class CaBaseAnalysisPermissionRegistry : CaAnalysisPermissionRegistry {
    private val threadLocalExplicitAnalysisRestriction: ThreadLocal<CaExplicitAnalysisRestriction?> =
        ThreadLocal.withInitial { null }

    private val threadLocalAllowOnEdt: ThreadLocal<Boolean> =
        ThreadLocal.withInitial { false }

    private val threadLocalAllowInWriteAction: ThreadLocal<Boolean> =
        ThreadLocal.withInitial { false }

    override var explicitAnalysisRestriction: CaExplicitAnalysisRestriction?
        get() = threadLocalExplicitAnalysisRestriction.get()
        set(value) = threadLocalExplicitAnalysisRestriction.set(value)

    override var isAnalysisAllowedOnEdt: Boolean
        get() = threadLocalAllowOnEdt.get()
        set(value) = threadLocalAllowOnEdt.set(value)

    override var isAnalysisAllowedInWriteAction: Boolean
        get() = threadLocalAllowInWriteAction.get()
        set(value) = threadLocalAllowInWriteAction.set(value)
}
