package org.cangnova.cangjie.analysis.api.platform.lifetime

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.platform.CaCachedService
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import kotlin.reflect.KClass

/**
 * IDE 宿主的 read-action 约束 lifetime token。
 *
 * 对齐 Kotlin `KotlinReadActionConfinementLifetimeToken`：
 * 1. 有效性绑定到底层 modification tracker；
 * 2. 可访问性同时受 read action、权限检查器与当前 analyze 上下文约束。
 */
@CaPlatformInterface
class CaReadActionConfinementLifetimeToken(
    project: Project,
    private val modificationTracker: ModificationTracker,
) : CaLifetimeToken() {
    private val onCreatedTimeStamp = modificationTracker.modificationCount

    @CaCachedService
    private val permissionChecker = CaAnalysisPermissionChecker.getInstance(project)

    @CaCachedService
    private val lifetimeTracker = CaLifetimeTracker.getInstance(project)

    override fun isValid(): Boolean {
        return onCreatedTimeStamp == modificationTracker.modificationCount
    }

    @OptIn(CaImplementationDetail::class)
    override fun getInvalidationReason(): String {
        if (onCreatedTimeStamp != modificationTracker.modificationCount) {
            return if (modificationTracker is ModificationTrackerWithInvalidationReason) {
                val trackerInvalidationReason = modificationTracker.getInvalidationReason()
                    ?: error("Cannot get an invalidation reason for a ${ModificationTrackerWithInvalidationReason::class.simpleName} that's valid.")
                "Session is invalidated: $trackerInvalidationReason"
            } else {
                "Session is invalidated"
            }
        }
        error("Cannot get an invalidation reason for a valid lifetime token.")
    }

    override fun isAccessible(): Boolean {
        if (!ApplicationManager.getApplication().isReadAccessAllowed) return false
        if (!permissionChecker.isAnalysisAllowed()) return false

        return lifetimeTracker.currentToken == this
    }

    override fun getInaccessibilityReason(): String {
        if (!ApplicationManager.getApplication().isReadAccessAllowed) return "Called outside a read action."
        if (!permissionChecker.isAnalysisAllowed()) return permissionChecker.getRejectionReason()

        val currentToken = lifetimeTracker.currentToken
        if (currentToken == null) return "Called outside an `analyze` context."
        if (currentToken != this) return "Using a lifetime owner from an old `analyze` context."

        error("Cannot get an inaccessibility reason for a lifetime token when it's accessible.")
    }
}

@CaPlatformInterface
class CaReadActionConfinementLifetimeTokenFactory : CaLifetimeTokenFactory {
    override val identifier: KClass<out CaLifetimeToken> = CaReadActionConfinementLifetimeToken::class

    override fun create(project: Project, modificationTracker: ModificationTracker): CaLifetimeToken =
        CaReadActionConfinementLifetimeToken(project, modificationTracker)
}
