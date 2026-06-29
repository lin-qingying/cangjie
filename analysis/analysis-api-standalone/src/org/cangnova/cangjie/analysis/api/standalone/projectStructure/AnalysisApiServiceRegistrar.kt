package org.cangnova.cangjie.analysis.api.standalone.projectStructure

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable

/**
 * 服务注册器接口（对齐 Kotlin 的 AnalysisApiServiceRegistrar）。
 *
 * 定义将 Analysis API 服务注册到 [MockApplication]/[MockProject] 的规范。
 * 通常与 [org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider] 配合使用，从 XML 描述文件加载服务。
 *
 * @param DATA 由设置过程提供给注册器的额外信息。
 */
interface AnalysisApiServiceRegistrar<in DATA> {
    /**
     * 注册 mock application 生命周期内共享的应用级服务。
     */
    fun registerApplicationServices(application: MockApplication, data: DATA)

    /**
     * 注册当前 project 需要声明的扩展点。
     */
    fun registerProjectExtensionPoints(project: MockProject, data: DATA)

    /**
     * 注册当前 project 级别的 Analysis API 服务实现。
     */
    fun registerProjectServices(project: MockProject, data: DATA)

    /**
     * 注册依赖 project model 或测试模型 disposable 生命周期的服务。
     */
    fun registerProjectModelServices(project: MockProject, disposable: Disposable, data: DATA)
}

/**
 * 以应用服务去重语义执行一组注册器的 application-service 注册。
 */
fun <T> List<AnalysisApiServiceRegistrar<T>>.registerApplicationServices(application: MockApplication, data: T) {
    ApplicationServiceRegistration.register(application, this, data)
}

/**
 * 顺序执行一组注册器的 project-extension-point 注册。
 */
fun <T> List<AnalysisApiServiceRegistrar<T>>.registerProjectExtensionPoints(project: MockProject, data: T) {
    forEach { it.registerProjectExtensionPoints(project, data) }
}

/**
 * 顺序执行一组注册器的 project-service 注册。
 */
fun <T> List<AnalysisApiServiceRegistrar<T>>.registerProjectServices(project: MockProject, data: T) {
    forEach { it.registerProjectServices(project, data) }
}

/**
 * 顺序执行一组注册器中依赖 project model 生命周期的服务注册。
 */
fun <T> List<AnalysisApiServiceRegistrar<T>>.registerProjectModelServices(project: MockProject, disposable: Disposable, data: T) {
    forEach { it.registerProjectModelServices(project, disposable, data) }
}

/**
 * 不需要额外数据的简化注册器（对齐 Kotlin 的 AnalysisApiSimpleServiceRegistrar）。
 */
abstract class AnalysisApiSimpleServiceRegistrar : AnalysisApiServiceRegistrar<Any> {
    /**
     * 注册不需要额外数据的 application 级服务。
     */
    open fun registerApplicationServices(application: MockApplication) {}

    /**
     * 注册不需要额外数据的 project 扩展点。
     */
    open fun registerProjectExtensionPoints(project: MockProject) {}

    /**
     * 注册不需要额外数据的 project 级服务。
     */
    open fun registerProjectServices(project: MockProject) {}

    /**
     * 注册不需要额外数据但绑定 project model 生命周期的服务。
     */
    open fun registerProjectModelServices(project: MockProject, disposable: Disposable) {}

    /**
     * 将带数据的接口调用转接到无数据 application-service 注册入口。
     */
    final override fun registerApplicationServices(application: MockApplication, data: Any) {
        registerApplicationServices(application)
    }

    /**
     * 将带数据的接口调用转接到无数据 project-extension-point 注册入口。
     */
    final override fun registerProjectExtensionPoints(project: MockProject, data: Any) {
        registerProjectExtensionPoints(project)
    }

    /**
     * 将带数据的接口调用转接到无数据 project-service 注册入口。
     */
    final override fun registerProjectServices(project: MockProject, data: Any) {
        registerProjectServices(project)
    }

    /**
     * 将带数据的接口调用转接到无数据 project-model-service 注册入口。
     */
    final override fun registerProjectModelServices(project: MockProject, disposable: Disposable, data: Any) {
        registerProjectModelServices(project, disposable)
    }
}
