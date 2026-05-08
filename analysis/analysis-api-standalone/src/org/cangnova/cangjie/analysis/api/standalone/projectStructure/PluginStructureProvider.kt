package org.cangnova.cangjie.analysis.api.standalone.projectStructure

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.ide.plugins.DataLoader
import com.intellij.ide.plugins.PluginXmlPathResolver
import com.intellij.mock.MockApplication
import com.intellij.mock.MockComponentManager
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.extensions.ExtensionDescriptor
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.platform.plugins.parser.impl.PluginDescriptorReaderContext
import com.intellij.platform.plugins.parser.impl.RawPluginDescriptor
import com.intellij.platform.plugins.parser.impl.ScopedElementsContainer
import com.intellij.platform.plugins.parser.impl.elements.ExtensionElement
import com.intellij.platform.plugins.parser.impl.elements.OS
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.xml.dom.NoOpXmlInterner
import java.io.InputStream

/**
 * 为 standalone/headless 容器装配 plugin XML 中声明的结构信息。
 *
 * 这里对齐 Kotlin standalone `PluginStructureProvider` 的职责边界：
 * 1. 从 plugin XML 读取 extension point / extension / service 声明；
 * 2. 将这些声明注册进 headless `MockApplication` / `MockProject`；
 * 3. 让 standalone、LSP、测试环境共享同一套 XML 驱动的装配流程。
 *
 * 253 版本 IntelliJ 将插件描述符解析模型拆到了 `plugins-parser-impl`，
 * 因此这里不能再沿用旧的 `ContainerDescriptor + RawPluginDescriptor(epNameToExtensions)` 路径，
 * 而要直接使用新的 `PluginDescriptorBuilder / RawPluginDescriptor / ScopedElementsContainer` 模型。
 */
@Suppress("UnstableApiUsage")
object PluginStructureProvider {
    private val fakePluginDescriptor = DefaultPluginDescriptor("cangjie-analysis-api-loader")

    /**
     * 统一 standalone 场景的插件 XML 读取上下文。
     *
     * 当前我们要求：
     * 1. 使用无副作用的 XML interner；
     * 2. 对缺失 include 明确报错，而不是静默忽略。
     */
    private object ReadContext : PluginDescriptorReaderContext {
        override val interner get() = NoOpXmlInterner
        override val isMissingIncludeIgnored: Boolean get() = false
    }

    /**
     * 通过类加载器从资源路径读取 plugin XML。
     *
     * `PathResolver` 在 253 中读取的是不带前导 `/` 的资源路径，
     * 因此这里集中做一次规范化，避免调用侧反复记忆细节。
     */
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
        val containerDescriptor = RawPluginDescriptor::appElementsContainer

        registerExtensionPoints(application, pluginRelativePath, containerDescriptor)
        registerExtensionPointImplementations(application, pluginRelativePath)
        registerServices(application, pluginRelativePath, containerDescriptor)
    }

    fun registerProjectServices(project: MockProject, pluginRelativePath: String) {
        val containerDescriptor = RawPluginDescriptor::projectElementsContainer

        registerExtensionPoints(project, pluginRelativePath, containerDescriptor)
        registerExtensionPointImplementations(project, pluginRelativePath)
        registerServices(project, pluginRelativePath, containerDescriptor)
    }

    private inline fun registerExtensionPoints(
        componentManager: MockComponentManager,
        pluginRelativePath: String,
        containerDescriptor: RawPluginDescriptor.() -> ScopedElementsContainer,
    ) {
        val pluginDescriptor = getOrCalculatePluginDescriptor(PluginDesignation(pluginRelativePath, componentManager))
        for (extensionPointDescriptor in pluginDescriptor.containerDescriptor().extensionPoints) {
            val extensionPointName = extensionPointDescriptor.qualifiedName ?: extensionPointDescriptor.name ?: continue
            if (extensionPointName in forbiddenExtensionPointNames) continue

            val extensionPointClassName = extensionPointDescriptor.beanClass ?: extensionPointDescriptor.`interface` ?: continue
            CoreApplicationEnvironment.registerExtensionPoint(
                componentManager.extensionArea,
                extensionPointName,
                componentManager.loadClass<Any>(extensionPointClassName, fakePluginDescriptor),
            )
        }
    }

    private inline fun registerServices(
        componentManager: MockComponentManager,
        pluginRelativePath: String,
        containerDescriptor: RawPluginDescriptor.() -> ScopedElementsContainer,
    ) {
        val pluginDescriptor = getOrCalculatePluginDescriptor(PluginDesignation(pluginRelativePath, componentManager))
        for (serviceDescriptor in pluginDescriptor.containerDescriptor().services) {
            val serviceImplementationName = serviceDescriptor.serviceImplementation ?: continue
            val serviceImplementationClass = componentManager.loadClass<Any>(serviceImplementationName, fakePluginDescriptor)
            val serviceInterfaceName = serviceDescriptor.serviceInterface?.takeUnless(String::isBlank)
            if (serviceInterfaceName != null) {
                val serviceInterfaceClass = componentManager.loadClass<Any>(serviceInterfaceName, fakePluginDescriptor)
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
        val extensionPointImplementations = pluginDescriptor.extensions
        for (allowedExtensionPointName in allowedExtensionPointNames) {
            val point = componentManager.extensionArea.getExtensionPointIfRegistered<Any>(allowedExtensionPointName) ?: continue
            val descriptors = extensionPointImplementations[allowedExtensionPointName]
                ?.map { extensionElement -> extensionElement.toExtensionDescriptor() }
                ?: continue
            point.registerExtensions(descriptors, fakePluginDescriptor, null)
        }
    }

    private fun getOrCalculatePluginDescriptor(
        designation: PluginDesignation,
    ): RawPluginDescriptor {
        return pluginDescriptorsCache.computeIfAbsent(designation) {
            val descriptorBuilder = PluginXmlPathResolver.DEFAULT_PATH_RESOLVER.resolvePath(
                ReadContext,
                ResourceDataLoader(designation.classLoader),
                designation.relativePath,
            ) ?: error("无法从 ${designation.relativePath} 解析 plugin XML 描述符")
            descriptorBuilder.build()
        }
    }

    private fun ExtensionElement.toExtensionDescriptor(): ExtensionDescriptor {
        val loadingOrder = order
            ?.takeIf(String::isNotBlank)
            ?.let(LoadingOrder::readOrder)
            ?: LoadingOrder.ANY

        return ExtensionDescriptor(
            implementation,
            os?.toPlatformOs(),
            orderId,
            loadingOrder,
            element,
            hasExtraAttributes,
        )
    }

    private fun OS.toPlatformOs(): ExtensionDescriptor.Os {
        return when (this) {
            OS.MAC -> ExtensionDescriptor.Os.mac
            OS.LINUX -> ExtensionDescriptor.Os.linux
            OS.WINDOWS -> ExtensionDescriptor.Os.windows
            OS.UNIX -> ExtensionDescriptor.Os.unix
            OS.FREEBSD -> ExtensionDescriptor.Os.freebsd
        }
    }

    /**
     * 统一规范 headless 插件 XML 路径。
     *
     * 仓库里同时存在 `"META-INF/..."` 与 `"/META-INF/..."` 两种写法，
     * 这里统一为带前导 `/` 的 plugin-relative path，避免上层调用者依赖路径细节。
     */
    private fun normalizePluginXmlPath(path: String): String {
        return if (path.startsWith("/")) path else "/$path"
    }

    /**
     * headless 容器中禁止自动注册的扩展点名单。
     *
     * 这些扩展点通常依赖完整 IDE 生命周期或 UI 基础设施，
     * 不应该在 standalone 容器中被 XML 自动装配。
     */
    private val forbiddenExtensionPointNames = listOf<String>()

    /**
     * 当前允许在 headless/standalone 容器中自动装配的扩展点名单。
     *
     * 新增可安全装配的分析扩展时，应在这里扩展白名单，
     * 而不是回退到测试代码里手工逐个注册 extension 的退化写法。
     */
    private val allowedExtensionPointNames = listOf(
        "org.cangnova.cangjie.psiReferenceProvider",
        "com.intellij.filetype.decompiler",
        "com.intellij.referencesSearch",
        "com.intellij.lang.findUsagesProvider",
        "com.intellij.targetElementEvaluator",
        "com.intellij.targetElementUtilExtender",
        "com.intellij.usageTargetProvider",
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
