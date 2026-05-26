package org.cangnova.cangjie

import com.intellij.core.CoreProjectEnvironment
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.messages.impl.PluginListenerDescriptor
import org.picocontainer.PicoContainer

/**
 * 仓颉核心项目环境。
 *
 * 对齐 Kotlin `KotlinCoreProjectEnvironment` 的职责：
 * - 在项目容器创建期间尽早预注册项目级扩展点
 * - 在基础 PSI 服务装配完成后补齐仓颉需要的项目级平台服务
 */
open class CangjieCoreProjectEnvironment(
    disposable: Disposable,
    applicationEnvironment: CangjieCoreApplicationEnvironment,
) : CoreProjectEnvironment(disposable, applicationEnvironment) {
    /**
     * 253 的 message bus 懒 listener 会回调 `MockProject.createListener`。
     *
     * 如果这里继续使用平台默认实现，`projectListeners` 一旦真正开始参与订阅，
     * 就会直接在 `MockComponentManager.createListener` 抛 `UnsupportedOperationException`。
     * 因此必须与 Kotlin standalone project factory 一样，在项目容器层负责把
     * plugin XML 中声明的 listener 物化成 `(Project) -> Listener` 构造结果。
     */
    override fun createProject(parent: PicoContainer, parentDisposable: Disposable): MockProject {
        return object : MockProject(parent, parentDisposable) {
            @Suppress("UnstableApiUsage")
            override fun createListener(descriptor: PluginListenerDescriptor): Any {
                val listenerClass = loadClass<Any>(descriptor.descriptor.listenerClassName, descriptor.pluginDescriptor)
                return listenerClass.getDeclaredConstructor(Project::class.java).newInstance(this)
            }
        }
    }

    override fun preregisterServices() {
        CangJieHeadlessPlatformBootstrap.preregisterProjectEnvironment(this)
    }

    init {
        CangJieHeadlessPlatformBootstrap.initializeProjectEnvironment(this)
    }
}
