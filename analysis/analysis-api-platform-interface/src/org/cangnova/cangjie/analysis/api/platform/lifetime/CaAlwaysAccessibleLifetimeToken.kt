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
    private val modificationTracker = CaModificationTracker.getInstance(project)
    private val onCreatedTimeStamp = modificationTracker?.modificationCount ?: 0L

    override fun isValid(): Boolean {
        return onCreatedTimeStamp == (modificationTracker?.modificationCount ?: 0L)
    }

    override fun getInvalidationReason(): String {
        if (onCreatedTimeStamp != (modificationTracker?.modificationCount ?: 0L)) return "PSI has changed since creation"
        error("Getting invalidation reason for valid validity token")
    }

    override fun isAccessible(): Boolean {
        return true
    }

    override fun getInaccessibilityReason(): String {
        error("Getting inaccessibility reason for validity token when it is accessible")
    }
}

@CaPlatformInterface
class CaAlwaysAccessibleLifetimeTokenFactory : CaLifetimeTokenFactory {
    override val identifier: KClass<out CaLifetimeToken> = CaAlwaysAccessibleLifetimeToken::class

    override fun create(project: Project, modificationTracker: ModificationTracker): CaLifetimeToken =
        CaAlwaysAccessibleLifetimeToken(project)
}
