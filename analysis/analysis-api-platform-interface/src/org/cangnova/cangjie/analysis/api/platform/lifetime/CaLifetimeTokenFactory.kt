package org.cangnova.cangjie.analysis.api.platform.lifetime

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import kotlin.reflect.KClass

/**
 * Analysis API 生命周期令牌工厂。
 *
 * 对齐 Kotlin `KotlinLifetimeTokenFactory`：
 * 平台必须基于给定的 `ModificationTracker` 创建 token，
 * 这样 analysis session 的有效性才能直接绑定到底层会话或宿主快照。
 */
@CaPlatformInterface
interface CaLifetimeTokenFactory {
    /**
     * 工厂创建的 lifetime token 类型标识。
     */
    val identifier: KClass<out CaLifetimeToken>

    /**
     * 基于具体分析会话对应的有效性跟踪器创建 token。
     */
    fun create(project: Project, modificationTracker: ModificationTracker): CaLifetimeToken

    companion object {
        /**
         * 获取项目级 lifetime token 工厂服务。
         */
        fun getInstance(project: Project): CaLifetimeTokenFactory = project.service()
    }
}
