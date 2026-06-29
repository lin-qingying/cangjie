

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.extensions.ProjectExtensionDescriptor

/**
 * low-level CFIR session 的工程级扩展配置接口。
 *
 * 插件可以通过该 extension point 在 session 创建完成后注册额外组件。
 */
interface LLCfirSessionConfigurator {
    companion object : ProjectExtensionDescriptor<LLCfirSessionConfigurator>(
        "org.cangnova.cangjie.llCfirSessionConfigurator",
        LLCfirSessionConfigurator::class.java
    )

    /**
     * 对 [session] 执行扩展配置。
     */
    fun configure(session: LLCfirSession)
}
