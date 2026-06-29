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
    /**
     * token 所属 project。
     */
    private val project: Project,
    /**
     * 用于判断 token 是否失效的修改计数器。
     */
    private val modificationTracker: ModificationTracker,
) : CaLifetimeToken() {
    /**
     * token 创建时捕获的修改计数。
     */
    private val onCreatedTimeStamp = modificationTracker.modificationCount

    /**
     * 当前 project 的分析权限检查服务。
     */
    @CaCachedService
    private val permissionChecker = CaAnalysisPermissionChecker.getInstance(project)

    /**
     * 当前 project 的 lifetime tracker。
     */
    @CaCachedService
    private val lifetimeTracker = CaLifetimeTracker.getInstance(project)

    /**
     * 判断 token 对应 session 是否仍未被修改计数失效。
     */
    override fun isValid(): Boolean = onCreatedTimeStamp == modificationTracker.modificationCount

    /**
     * 返回 token 失效原因。
     */
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

    /**
     * 判断当前线程是否允许访问该 token 绑定的 lifetime owner。
     */
    override fun isAccessible(): Boolean {
        if (!permissionChecker.isAnalysisAllowed()) return false
        return lifetimeTracker.currentToken === this
    }

    /**
     * 返回当前 token 不可访问的原因。
     */
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

/**
 * 创建 [CaBaseLifetimeToken] 的平台 token factory。
 */
@OptIn(CaPlatformInterface::class)
internal class CaBaseLifetimeTokenFactory : org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTokenFactory {
    /**
     * 该 factory 创建的 token 类型标识。
     */
    override val identifier: KClass<out CaLifetimeToken> = CaBaseLifetimeToken::class

    /**
     * 为给定 project 与修改计数器创建 lifetime token。
     */
    override fun create(project: Project, modificationTracker: ModificationTracker): CaLifetimeToken {
        return CaBaseLifetimeToken(project, modificationTracker)
    }
}
