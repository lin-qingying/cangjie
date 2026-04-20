package org.cangnova.cangjie.analysis.api.impl.base.lifetime

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.platform.CaCachedService
import org.cangnova.cangjie.analysis.api.platform.lifetime.ModificationTrackerWithInvalidationReason
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTracker
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import kotlin.reflect.KClass

/**
 * Analysis API 基础生命周期令牌。
 *
 * 该实现承担两类职责：
 * 1. 跟踪对象是否已失效
 * 2. 校验当前线程是否处于该 token 对应的分析上下文中
 *
 * 这与 Kotlin Analysis API 中 token 既管理 validity，
 * 又管理 analysis context accessibility 的职责保持一致。
 */
internal class CaBaseLifetimeToken(
    private val project: Project,
    private val modificationTracker: ModificationTracker,
) : CaLifetimeToken() {
    private val onCreatedTimeStamp = modificationTracker.modificationCount

    @CaCachedService
    private val permissionChecker = CaAnalysisPermissionChecker.getInstance(project)

    @CaCachedService
    private val lifetimeTracker = CaLifetimeTracker.getInstance(project)

    override fun isValid(): Boolean = onCreatedTimeStamp == modificationTracker.modificationCount

    @OptIn(CaImplementationDetail::class)
    override fun getInvalidationReason(): String {
        if (onCreatedTimeStamp != modificationTracker.modificationCount) {
            return if (modificationTracker is ModificationTrackerWithInvalidationReason) {
                val trackerInvalidationReason = modificationTracker.getInvalidationReason()
                    ?: error("Cannot get an invalidation reason for a valid ${ModificationTrackerWithInvalidationReason::class.simpleName}")
                "Session is invalidated: $trackerInvalidationReason"
            } else {
                "Session is invalidated"
            }
        }
        error("Cannot get an invalidation reason for a valid lifetime token.")
    }

    override fun isAccessible(): Boolean {
        if (!permissionChecker.isAnalysisAllowed()) return false
        return lifetimeTracker.currentToken === this
    }

    override fun getInaccessibilityReason(): String {
        if (!permissionChecker.isAnalysisAllowed()) {
            return permissionChecker.getRejectionReason()
        }

        val currentToken = lifetimeTracker.currentToken
        if (currentToken == null) return "Called outside an `analyze` context."
        if (currentToken !== this) return "Using a lifetime owner from an old `analyze` context."

        error("Cannot get an inaccessibility reason for a lifetime token when it's accessible.")
    }
}

@OptIn(CaPlatformInterface::class)
internal class CaBaseLifetimeTokenFactory : org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTokenFactory {
    override val identifier: KClass<out CaLifetimeToken> = CaBaseLifetimeToken::class

    override fun create(project: Project, modificationTracker: ModificationTracker): CaLifetimeToken {
        return CaBaseLifetimeToken(project, modificationTracker)
    }
}
