package org.cangnova.cangjie.analysis.api.impl.base.permissions

import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry.CaExplicitAnalysisRestriction

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
