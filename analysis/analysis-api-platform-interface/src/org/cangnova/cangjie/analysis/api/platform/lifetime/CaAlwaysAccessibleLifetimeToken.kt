package org.cangnova.cangjie.analysis.api.platform.lifetime

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import kotlin.reflect.KClass

/**
 * Standalone 宿主的 always-accessible lifetime token。
 *
 * 对齐 Kotlin `KotlinAlwaysAccessibleLifetimeToken`：
 * standalone 宿主不要求 read action 约束，可访问性恒为 true，
 * 仅根据 project-wide modification 版本失效。
 */
@CaPlatformInterface
class CaAlwaysAccessibleLifetimeToken(project: Project) : CaLifetimeToken() {
    /**
     * 用于判断 token 是否仍有效的项目修改追踪器。
     */
    private val modificationTracker = CaModificationTracker.getInstance(project)
    /**
     * token 创建时记录的修改计数。
     */
    private val onCreatedTimeStamp = modificationTracker?.modificationCount ?: 0L

    /**
     * standalone token 在项目修改计数未变化时有效。
     */
    override fun isValid(): Boolean {
        return onCreatedTimeStamp == (modificationTracker?.modificationCount ?: 0L)
    }

    /**
     * 返回 token 因项目 PSI 变化失效的原因。
     */
    override fun getInvalidationReason(): String {
        if (onCreatedTimeStamp != (modificationTracker?.modificationCount ?: 0L)) return "PSI has changed since creation"
        error("Getting invalidation reason for valid validity token")
    }

    /**
     * standalone token 始终可访问。
     */
    override fun isAccessible(): Boolean {
        return true
    }

    /**
     * standalone token 可访问时不应请求不可访问原因。
     */
    override fun getInaccessibilityReason(): String {
        error("Getting inaccessibility reason for validity token when it is accessible")
    }
}

/**
 * standalone always-accessible lifetime token 工厂。
 */
@CaPlatformInterface
class CaAlwaysAccessibleLifetimeTokenFactory : CaLifetimeTokenFactory {
    /**
     * 该工厂创建的 token 类型标识。
     */
    override val identifier: KClass<out CaLifetimeToken> = CaAlwaysAccessibleLifetimeToken::class

    /**
     * 创建 standalone always-accessible token。
     */
    override fun create(project: Project, modificationTracker: ModificationTracker): CaLifetimeToken =
        CaAlwaysAccessibleLifetimeToken(project)
}
