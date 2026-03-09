package org.cangjie.analysis.api.impl.base.projectStructure

import com.intellij.ide.plugins.ContainerDescriptor
import com.intellij.ide.plugins.PluginXmlPathResolver
import com.intellij.ide.plugins.RawPluginDescriptor
import com.intellij.ide.plugins.ReadModuleContext
import com.intellij.mock.MockApplication
import com.intellij.mock.MockComponentManager
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.util.xml.dom.NoOpXmlInterner
import java.io.InputStream

/**
 * XML 服务描述文件加载器（对齐 Kotlin 的 PluginStructureProvider）。
 *
 * 从 classpath 上的 XML 文件中解析 service 声明，
 * 通过反射注册到 [MockApplication]/[MockProject]（绕过 Kotlin `internal` 限制）。
 */
@Suppress("UnstableApiUsage")
object PluginStructureProvider {
    private val fakePluginDescriptor = DefaultPluginDescriptor("cangjie-analysis-api-loader")

    private object ReadContext : ReadModuleContext {
        override val interner get() = NoOpXmlInterner
        override val isMissingIncludeIgnored: Boolean get() = false
    }

    private class ResourceDataLoader(val classLoader: ClassLoader) : com.intellij.ide.plugins.DataLoader {
        override fun load(path: String, pluginDescriptorSourceOnly: Boolean): InputStream? =
            classLoader.getResource(path)?.openStream()

        override fun toString(): String = "resources data loader"
    }

    private fun loadPluginDescriptor(
        pluginRelativePath: String,
        componentManager: MockComponentManager,
    ): RawPluginDescriptor {
        return PluginXmlPathResolver.DEFAULT_PATH_RESOLVER.resolvePath(
            readContext = ReadContext,
            dataLoader = ResourceDataLoader(componentManager.javaClass.classLoader),
            relativePath = pluginRelativePath,
            readInto = null,
        ) ?: RawPluginDescriptor()
    }

    fun registerApplicationServices(application: MockApplication, pluginRelativePath: String) {
        registerServices(application, pluginRelativePath, RawPluginDescriptor::appContainerDescriptor)
    }

    fun registerProjectServices(project: MockProject, pluginRelativePath: String) {
        registerServices(project, pluginRelativePath, RawPluginDescriptor::projectContainerDescriptor)
    }

    private inline fun registerServices(
        componentManager: MockComponentManager,
        pluginRelativePath: String,
        containerDescriptor: RawPluginDescriptor.() -> ContainerDescriptor,
    ) {
        val pluginDescriptor = loadPluginDescriptor(pluginRelativePath, componentManager)
        for (serviceDescriptor in pluginDescriptor.containerDescriptor().services) {
            val serviceImplementationClass =
                componentManager.loadClass<Any>(serviceDescriptor.serviceImplementation, fakePluginDescriptor)
            val serviceInterface = serviceDescriptor.serviceInterface
            if (serviceInterface != null) {
                val serviceInterfaceClass =
                    componentManager.loadClass<Any>(serviceInterface, fakePluginDescriptor)
                @Suppress("UNCHECKED_CAST")
                componentManager.registerServiceWithInterface(serviceInterfaceClass  , serviceImplementationClass as Class<Any>)
            } else {
                componentManager.registerService(serviceImplementationClass)
            }
        }
    }
    // workaround for ambiguity resolution
    private fun <T> MockComponentManager.registerServiceWithInterface(interfaceClass: Class<T>, implementationClass: Class<T>) {
        registerService(interfaceClass, implementationClass)
    }
}
