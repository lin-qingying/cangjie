package org.cangnova.cangjie.analysis.test.framework.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.registerApplicationServices
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.registerProjectExtensionPoints
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.registerProjectServices
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.impl.testConfiguration
import org.cangnova.cangjie.test.services.TestServices

/**
 * Analysis API 测试服务注册器基类。
 *
 * 它将 [AnalysisApiServiceRegistrar] 的数据类型固定为 [TestServices]，
 * 让测试 configurator 可以直接围绕测试服务容器组织服务注册流程。
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
abstract class AnalysisApiTestServiceRegistrar : AnalysisApiServiceRegistrar<TestServices> {
    /**
     * 注册测试 application 级服务；默认没有额外注册。
     */
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {}

    /**
     * 注册测试 project 扩展点；默认没有额外注册。
     */
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {}

    /**
     * 注册测试 project 级服务；默认没有额外注册。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {}

    /**
     * 注册依赖测试 root disposable 生命周期的 project-model 服务；默认没有额外注册。
     */
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {}
}

/**
 * 使用测试配置中的 root disposable 执行一组 registrar 的 project-model 服务注册。
 */
@OptIn(TestInfrastructureInternals::class)
fun List<AnalysisApiServiceRegistrar<TestServices>>.registerProjectModelServices(
    project: MockProject,
    testServices: TestServices,
) {
    forEach { it.registerProjectModelServices(project, testServices.testConfiguration.rootDisposable, testServices) }
}

/**
 * 按 application、project extension point、project service、project-model service 的顺序注册全部测试服务。
 */
fun List<AnalysisApiServiceRegistrar<TestServices>>.registerAllServices(
    application: MockApplication,
    project: MockProject,
    testServices: TestServices,
) {
    registerApplicationServices(application, testServices)
    registerProjectExtensionPoints(project, testServices)
    registerProjectServices(project, testServices)
    registerProjectModelServices(project, testServices)
}
