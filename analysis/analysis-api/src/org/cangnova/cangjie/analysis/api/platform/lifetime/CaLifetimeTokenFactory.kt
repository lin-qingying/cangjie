package org.cangjie.analysis.api.platform.lifetime

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangjie.analysis.api.lifetime.CaLifetimeToken

/**
 * 生命周期令牌工厂（对齐 Kotlin 的 KotlinLifetimeTokenFactory）。
 *
 * 为分析会话创建 [CaLifetimeToken]。
 */
interface CaLifetimeTokenFactory {
    fun create(project: Project): CaLifetimeToken

    companion object {
        fun getInstance(project: Project): CaLifetimeTokenFactory = project.service()
    }
}
