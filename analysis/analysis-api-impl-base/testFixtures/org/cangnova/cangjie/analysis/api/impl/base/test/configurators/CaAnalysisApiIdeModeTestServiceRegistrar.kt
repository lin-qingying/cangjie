@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.impl.base.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTokenFactory
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaReadActionConfinementLifetimeTokenFactory
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionOptions
import org.cangnova.cangjie.analysis.api.platform.permissions.CaDefaultAnalysisPermissionOptions
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * IDE mode Analysis API 测试宿主覆盖层。
 *
 * 对齐 Kotlin `AnalysisApiIdeModeTestServiceRegistrar` 的模块与职责归属：
 * - 应用级注册默认权限选项；
 * - 项目级注册 read-action confinement lifetime token factory。
 */
object CaAnalysisApiIdeModeTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * 注册 IDE mode 在项目级需要的 lifetime token factory。
     *
     * 该服务让 Analysis API session 的生命周期受 read action 约束，模拟 IDE 宿主中的访问规则。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(CaLifetimeTokenFactory::class.java, CaReadActionConfinementLifetimeTokenFactory::class.java)
        }
    }

    /**
     * 注册 IDE mode 在应用级需要的分析权限选项。
     *
     * 默认权限选项用于测试 read/write action、restricted analysis 等 IDE 访问边界。
     */
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        application.apply {
            registerService(CaAnalysisPermissionOptions::class.java, CaDefaultAnalysisPermissionOptions::class.java)
        }
    }
}
