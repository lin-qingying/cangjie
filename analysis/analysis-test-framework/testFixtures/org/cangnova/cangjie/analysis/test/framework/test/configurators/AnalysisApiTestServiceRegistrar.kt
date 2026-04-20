package org.cangnova.cangjie.analysis.test.framework.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * Analysis API 测试服务注册器基类。
 *
 * 它将 [AnalysisApiServiceRegistrar] 的数据类型固定为 [TestServices]，
 * 让测试 configurator 可以直接围绕测试服务容器组织服务注册流程。
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
abstract class AnalysisApiTestServiceRegistrar : AnalysisApiServiceRegistrar<TestServices> {
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {}

    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {}

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {}

    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {}
}

fun List<AnalysisApiServiceRegistrar<TestServices>>.registerApplicationServices(
    application: MockApplication,
    testServices: TestServices,
) {
    forEach { it.registerApplicationServices(application, testServices) }
}

fun List<AnalysisApiServiceRegistrar<TestServices>>.registerProjectExtensionPoints(
    project: MockProject,
    testServices: TestServices,
) {
    forEach { it.registerProjectExtensionPoints(project, testServices) }
}

fun List<AnalysisApiServiceRegistrar<TestServices>>.registerProjectServices(
    project: MockProject,
    testServices: TestServices,
) {
    forEach { it.registerProjectServices(project, testServices) }
}

fun List<AnalysisApiServiceRegistrar<TestServices>>.registerProjectModelServices(
    project: MockProject,
    disposable: Disposable,
    testServices: TestServices,
) {
    forEach { it.registerProjectModelServices(project, disposable, testServices) }
}

fun List<AnalysisApiServiceRegistrar<TestServices>>.registerAllServices(
    application: MockApplication,
    project: MockProject,
    disposable: Disposable,
    testServices: TestServices,
) {
    registerApplicationServices(application, testServices)
    registerProjectExtensionPoints(project, testServices)
    registerProjectServices(project, testServices)
    registerProjectModelServices(project, disposable, testServices)
}
