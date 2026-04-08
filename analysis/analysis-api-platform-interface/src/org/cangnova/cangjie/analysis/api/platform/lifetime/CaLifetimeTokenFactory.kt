package org.cangnova.cangjie.analysis.api.platform.lifetime

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken

/**
 * Analysis API 生命周期令牌工厂。
 *
 * 平台通过它决定具体令牌的形态与有效性约束，例如：
 * - IDE 下绑定到项目级 tracker。
 * - Standalone 下绑定到一次构建好的分析上下文。
 * - LSP 下绑定到文档快照或工作区版本。
 */
interface CaLifetimeTokenFactory {
    fun create(project: Project): CaLifetimeToken

    companion object {
        fun getInstance(project: Project): CaLifetimeTokenFactory = project.service()
    }
}
