package org.cangnova.cangjie.analysis.api.impl.base.projectStructure

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.ide.plugins.ContainerDescriptor
import com.intellij.ide.plugins.DataLoader
import com.intellij.ide.plugins.PluginXmlPathResolver
import com.intellij.ide.plugins.RawPluginDescriptor
import com.intellij.ide.plugins.ReadModuleContext
import com.intellij.mock.MockApplication
import com.intellij.mock.MockComponentManager
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.xml.dom.NoOpXmlInterner
import java.io.InputStream

/**
 * 对齐 Kotlin standalone `PluginStructureProvider` 的 headless 插件结构装配器。
 *
 * 之前这里只有 service 注册能力，无法承载 Kotlin `kt-references` 同构所需的：
 * 1. 自定义 extension point 注册；
 * 2. extension implementation 装配；
 * 3. 与 plugin XML 同源的 headless/standalone 测试容器构建。
 *
 * 这会导致 `psiReferenceProvider` 这类 contributor 架构在测试和 standalone 环境里
 * “XML 写了但从未真正生效”。因此这里必须先补足完整插件结构装配能力。
 */
@Suppress("UnstableApiUsage")
object PluginStructureProvider {
    private val fakePluginDescriptor = DefaultPluginDescriptor("cangjie-analysis-api-loader")

    private object ReadContext : ReadModuleContext {
        override val interner get() = NoOpXmlInterner
        override val isMissingIncludeIgnored: Boolean get() = false
    }

    private class ResourceDataLoader(
        private val classLoader: ClassLoader,
    ) : DataLoader {
        override fun load(path: String, pluginDescriptorSourceOnly: Boolean): InputStream? {
            val normalizedPath = path.removePrefix("/")
            return classLoader.getResource(normalizedPath)?.openStream()
        }

        override fun toString(): String = "resources data loader"
    }

    private val pluginDescriptorsCache = ContainerUtil.createConcurrentSoftKeySoftValueMap<PluginDesignation, RawPluginDescriptor>()

    private data class PluginDesignation(
        val relativePath: String,
        val classLoader: ClassLoader,
    ) {
        constructor(relativePath: String, componentManager: MockComponentManager) : this(
            normalizePluginXmlPath(relativePath),
            componentManager.classLoader,
        )
    }

    fun registerApplicationServices(application: MockApplication, pluginRelativePath: String) {
        val containerDescriptor = RawPluginDescriptor::appContainerDescriptor

        registerExtensionPoints(application, pluginRelativePath, containerDescriptor)
        registerExtensionPointImplementations(application, pluginRelativePath)
        registerServices(application, pluginRelativePath, containerDescriptor)
    }

    fun registerProjectServices(project: MockProject, pluginRelativePath: String) {
        val containerDescriptor = RawPluginDescriptor::projectContainerDescriptor

        registerExtensionPoints(project, pluginRelativePath, containerDescriptor)
        registerExtensionPointImplementations(project, pluginRelativePath)
        registerServices(project, pluginRelativePath, containerDescriptor)
    }

    private inline fun registerExtensionPoints(
        componentManager: MockComponentManager,
        pluginRelativePath: String,
        containerDescriptor: RawPluginDescriptor.() -> ContainerDescriptor,
    ) {
        val pluginDescriptor = getOrCalculatePluginDescriptor(PluginDesignation(pluginRelativePath, componentManager))
        for (extensionPointDescriptor in pluginDescriptor.containerDescriptor().extensionPoints.orEmpty()) {
            val extensionPointName = extensionPointDescriptor.name
            if (extensionPointName in forbiddenExtensionPointNames) continue

            CoreApplicationEnvironment.registerExtensionPoint(
                componentManager.extensionArea,
                extensionPointName,
                componentManager.loadClass<Any>(extensionPointDescriptor.className, fakePluginDescriptor),
            )
        }
    }

    private inline fun registerServices(
        componentManager: MockComponentManager,
        pluginRelativePath: String,
        containerDescriptor: RawPluginDescriptor.() -> ContainerDescriptor,
    ) {
        val pluginDescriptor = getOrCalculatePluginDescriptor(PluginDesignation(pluginRelativePath, componentManager))
        for (serviceDescriptor in pluginDescriptor.containerDescriptor().services) {
            val serviceImplementationClass = componentManager.loadClass<Any>(serviceDescriptor.serviceImplementation, fakePluginDescriptor)
            val serviceInterface = serviceDescriptor.serviceInterface
            if (serviceInterface != null) {
                val serviceInterfaceClass = componentManager.loadClass<Any>(serviceInterface, fakePluginDescriptor)
                @Suppress("UNCHECKED_CAST")
                componentManager.registerServiceWithInterface(serviceInterfaceClass, serviceImplementationClass)
            } else {
                componentManager.registerService(serviceImplementationClass)
            }
        }
    }

    private fun registerExtensionPointImplementations(
        componentManager: MockComponentManager,
        pluginRelativePath: String,
    ) {
        val pluginDescriptor = getOrCalculatePluginDescriptor(PluginDesignation(pluginRelativePath, componentManager))
        val extensionPointImplementations = pluginDescriptor.epNameToExtensions.orEmpty()
        for (allowedExtensionPointName in allowedExtensionPointNames) {
            val point = componentManager.extensionArea.getExtensionPointIfRegistered<Any>(allowedExtensionPointName) ?: continue
            val descriptors = extensionPointImplementations[allowedExtensionPointName] ?: continue
            point.registerExtensions(descriptors, fakePluginDescriptor, null)
        }
    }

    private fun getOrCalculatePluginDescriptor(
        designation: PluginDesignation,
    ): RawPluginDescriptor {
        return pluginDescriptorsCache.computeIfAbsent(designation) {
            PluginXmlPathResolver.DEFAULT_PATH_RESOLVER.resolvePath(
                readContext = ReadContext,
                dataLoader = ResourceDataLoader(designation.classLoader),
                relativePath = normalizePluginXmlPath(designation.relativePath),
                readInto = null,
            ) ?: RawPluginDescriptor()
        }
    }

    /**
     * 统一规整 headless 插件 XML 路径。
     *
     * 当前仓库里既有 `"META-INF/..."`，也有 `"/META-INF/..."` 两种写法；
     * 但 `PluginXmlPathResolver` 的稳定输入更接近带前导 `/` 的 plugin-relative path。
     * 这里集中规整，避免 LSP、Standalone、测试宿主各自记忆路径细节，导致同一份 XML
     * 在某些宿主中能装配、某些宿主中静默失效。
     */
    private fun normalizePluginXmlPath(path: String): String {
        return if (path.startsWith("/")) path else "/$path"
    }

    /**
     * 这里保留显式黑名单，避免 headless 容器自动注册那些依赖完整 IDE 基础设施的扩展点。
     */
    private val forbiddenExtensionPointNames = listOf<String>()

    /**
     * 当前 headless/standalone 容器允许自动装配的扩展点白名单。
     *
     * 后续若继续对齐 Kotlin 的 analysis 平台扩展能力，应把新的安全扩展点继续加到这里，
     * 而不是回退到“测试里手工 registerExtension”的碎片写法。
     */
    private val allowedExtensionPointNames = listOf(
        "org.cangnova.cangjie.psiReferenceProvider",
        "com.intellij.filetype.decompiler",
        "com.intellij.referencesSearch",
        "com.intellij.lang.findUsagesProvider",
    )

    private val MockComponentManager.classLoader: ClassLoader
        get() = loadClass<Any>(PluginDesignation::class.java.name, fakePluginDescriptor).classLoader

    private fun <T> MockComponentManager.registerServiceWithInterface(
        interfaceClass: Class<T>,
        implementationClass: Class<T>,
    ) {
        registerService(interfaceClass, implementationClass)
    }
}
