@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.cfir.test.cases.session.builder

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisService
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandaloneModificationTracker
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandaloneModuleProvider
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandalonePlatformState
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandaloneProjectStructureProvider
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandaloneResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandaloneRestrictedAnalysisService
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandaloneSessionInvalidationService
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * 为手写 standalone builder 测试补齐平台级 XML 服务。
 *
 * 直接调用 `CaStandaloneSessionBuilder` 时，必须和生产态一样把 standalone 平台服务
 * 注册进 headless project；否则 `CaStandalonePlatformState` 等 service 无法解析。
 */
object StandaloneBuilderPlatformTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * 本测试注册器没有额外 application 级服务，保留空实现以明确覆盖测试框架扩展点。
     */
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {}

    /**
     * 注册 standalone builder 手写测试依赖的平台级 project service。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.registerService(CaStandalonePlatformState::class.java)
        project.registerService(CaRestrictedAnalysisService::class.java, CaStandaloneRestrictedAnalysisService::class.java)
        project.registerService(CangJieProjectStructureProvider::class.java, CaStandaloneProjectStructureProvider::class.java)
        project.registerService(CaModuleProvider::class.java, CaStandaloneModuleProvider::class.java)
        project.registerService(CaResolutionScopeProvider::class.java, CaStandaloneResolutionScopeProvider::class.java)
        project.registerService(CaModificationTracker::class.java, CaStandaloneModificationTracker::class.java)
        project.registerService(CaSessionInvalidationService::class.java, CaStandaloneSessionInvalidationService::class.java)
    }
}
