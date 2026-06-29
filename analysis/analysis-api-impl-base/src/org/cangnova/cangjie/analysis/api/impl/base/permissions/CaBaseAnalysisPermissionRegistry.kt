package org.cangnova.cangjie.analysis.api.impl.base.permissions

import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry.CaExplicitAnalysisRestriction

/**
 * 分析权限注册表的基础实现。
 *
 * 基于线程本地变量保存当前线程内的分析限制与权限开关，避免跨线程污染。
 */
internal class CaBaseAnalysisPermissionRegistry : CaAnalysisPermissionRegistry {
    /**
     * 当前线程显式设置的分析限制。
     */
    private val threadLocalExplicitAnalysisRestriction: ThreadLocal<CaExplicitAnalysisRestriction?> =
        ThreadLocal.withInitial { null }

    /**
     * 当前线程是否临时允许 EDT 分析。
     */
    private val threadLocalAllowOnEdt: ThreadLocal<Boolean> =
        ThreadLocal.withInitial { false }

    /**
     * 当前线程是否临时允许写动作内分析。
     */
    private val threadLocalAllowInWriteAction: ThreadLocal<Boolean> =
        ThreadLocal.withInitial { false }

    /**
     * 当前线程的显式分析限制。
     */
    override var explicitAnalysisRestriction: CaExplicitAnalysisRestriction?
        get() = threadLocalExplicitAnalysisRestriction.get()
        set(value) = threadLocalExplicitAnalysisRestriction.set(value)

    /**
     * 当前线程是否允许 EDT 分析。
     */
    override var isAnalysisAllowedOnEdt: Boolean
        get() = threadLocalAllowOnEdt.get()
        set(value) = threadLocalAllowOnEdt.set(value)

    /**
     * 当前线程是否允许写动作内分析。
     */
    override var isAnalysisAllowedInWriteAction: Boolean
        get() = threadLocalAllowInWriteAction.get()
        set(value) = threadLocalAllowInWriteAction.set(value)
}
