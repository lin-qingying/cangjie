package org.cangnova.cangjie.analysis.api.impl.base.lifetime

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTokenFactory

/**
 * Analysis API 生命周期令牌工厂的基础实现。
 *
 * 由基础平台层统一创建 token，保证所有 session/lifetime owner
 * 的可访问性约束都遵循同一套规则。
 */
internal class CaBaseLifetimeTokenFactory : CaLifetimeTokenFactory {
    override fun create(project: Project): CaLifetimeToken {
        return CaBaseLifetimeToken(project)
    }
}
