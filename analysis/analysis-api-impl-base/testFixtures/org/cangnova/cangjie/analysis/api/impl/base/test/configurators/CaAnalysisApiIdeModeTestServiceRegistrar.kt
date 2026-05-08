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
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(CaLifetimeTokenFactory::class.java, CaReadActionConfinementLifetimeTokenFactory::class.java)
        }
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        application.apply {
            registerService(CaAnalysisPermissionOptions::class.java, CaDefaultAnalysisPermissionOptions::class.java)
        }
    }
}
