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
    /**
     * 与当前 analysis session 绑定的修改追踪器。
     */
    private val modificationTracker: ModificationTracker,
) : CaLifetimeToken() {
    /**
     * token 创建时记录的修改计数。
     */
    private val onCreatedTimeStamp = modificationTracker.modificationCount

    /**
     * 平台分析权限检查器。
     */
    @CaCachedService
    private val permissionChecker = CaAnalysisPermissionChecker.getInstance(project)

    /**
     * 当前 analyze 上下文的 lifetime tracker。
     */
    @CaCachedService
    private val lifetimeTracker = CaLifetimeTracker.getInstance(project)

    /**
     * 修改计数未变化时 token 有效。
     */
    override fun isValid(): Boolean {
        return onCreatedTimeStamp == modificationTracker.modificationCount
    }

    /**
     * 返回 token 因底层修改而失效的原因。
     */
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

    /**
     * 检查当前线程是否处于 read action、平台是否允许分析且当前 analyze 上下文匹配该 token。
     */
    override fun isAccessible(): Boolean {
        if (!ApplicationManager.getApplication().isReadAccessAllowed) return false
        if (!permissionChecker.isAnalysisAllowed()) return false

        return lifetimeTracker.currentToken == this
    }

    /**
     * 返回当前 token 不可访问的具体原因。
     */
    override fun getInaccessibilityReason(): String {
        if (!ApplicationManager.getApplication().isReadAccessAllowed) return "Called outside a read action."
        if (!permissionChecker.isAnalysisAllowed()) return permissionChecker.getRejectionReason()

        val currentToken = lifetimeTracker.currentToken
        if (currentToken == null) return "Called outside an `analyze` context."
        if (currentToken != this) return "Using a lifetime owner from an old `analyze` context."

        error("Cannot get an inaccessibility reason for a lifetime token when it's accessible.")
    }
}

/**
 * IDE read-action confinement lifetime token 工厂。
 */
@CaPlatformInterface
class CaReadActionConfinementLifetimeTokenFactory : CaLifetimeTokenFactory {
    /**
     * 该工厂创建的 token 类型标识。
     */
    override val identifier: KClass<out CaLifetimeToken> = CaReadActionConfinementLifetimeToken::class

    /**
     * 创建 read-action confinement token。
     */
    override fun create(project: Project, modificationTracker: ModificationTracker): CaLifetimeToken =
        CaReadActionConfinementLifetimeToken(project, modificationTracker)
}
