package org.cangnova.cangjie.analysis.api.impl.base.projectStructure

import com.intellij.mock.MockApplication
import com.intellij.mock.MockComponentManager
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * headless 容器中的插件结构装配器。
 *
 * Analysis API、CFIR、references、standalone 等能力已拆成独立模块，
 * 测试环境和 headless 容器必须复用同一套 `META-INF/analysis-api/` XML 装配协议。
 *
 * 该装配器直接从 classpath 解析插件 XML，并把服务注册到 `MockComponentManager`：
 * 1. 支持 `xi:include`；
 * 2. 支持 `applicationService` / `projectService`；
 * 3. 保持 XML 声明顺序；
 * 4. 避免再次回到“测试环境手工注册一批服务”的碎片化写法。
 */
object PluginStructureProvider {
    private val fakePluginDescriptor = DefaultPluginDescriptor("cangjie-analysis-api-loader")

    private enum class ServiceContainerTag(val tagName: String) {
        APPLICATION("applicationService"),
        PROJECT("projectService"),
    }

    private data class ServiceDescriptor(
        val containerTag: ServiceContainerTag,
        val serviceInterface: String?,
        val serviceImplementation: String,
    )

    fun registerApplicationServices(application: MockApplication, pluginRelativePath: String) {
        registerServices(application, pluginRelativePath, ServiceContainerTag.APPLICATION)
    }

    fun registerProjectServices(project: MockProject, pluginRelativePath: String) {
        registerServices(project, pluginRelativePath, ServiceContainerTag.PROJECT)
    }

    private fun registerServices(
        componentManager: MockComponentManager,
        pluginRelativePath: String,
        containerTag: ServiceContainerTag,
    ) {
        val classLoader = componentManager.javaClass.classLoader
        val serviceDescriptors = loadServiceDescriptors(
            pluginRelativePath = normalizeResourcePath(pluginRelativePath),
            classLoader = classLoader,
            visitedResources = linkedSetOf(),
        )

        serviceDescriptors
            .asSequence()
            .filter { descriptor -> descriptor.containerTag == containerTag }
            .forEach { descriptor ->
                val implementationClass = componentManager.loadClass<Any>(
                    descriptor.serviceImplementation,
                    fakePluginDescriptor,
                )

                val serviceInterface = descriptor.serviceInterface
                if (serviceInterface != null) {
                    val serviceInterfaceClass = componentManager.loadClass<Any>(serviceInterface, fakePluginDescriptor)
                    @Suppress("UNCHECKED_CAST")
                    componentManager.registerServiceWithInterface(
                        serviceInterfaceClass,
                        implementationClass as Class<Any>,
                    )
                } else {
                    componentManager.registerService(implementationClass)
                }
            }
    }

    private fun loadServiceDescriptors(
        pluginRelativePath: String,
        classLoader: ClassLoader,
        visitedResources: MutableSet<String>,
    ): List<ServiceDescriptor> {
        if (!visitedResources.add(pluginRelativePath)) {
            return emptyList()
        }

        val rootElement = loadRootElement(pluginRelativePath, classLoader) ?: return emptyList()
        return collectServiceDescriptors(rootElement, pluginRelativePath, classLoader, visitedResources)
    }

    private fun collectServiceDescriptors(
        rootElement: Element,
        currentResourcePath: String,
        classLoader: ClassLoader,
        visitedResources: MutableSet<String>,
    ): List<ServiceDescriptor> {
        val descriptors = mutableListOf<ServiceDescriptor>()
        val childNodes = rootElement.childNodes

        for (index in 0 until childNodes.length) {
            val child = childNodes.item(index)
            if (child.nodeType != Node.ELEMENT_NODE) {
                continue
            }

            val childElement = child as Element
            when {
                childElement.matchesTag("include") -> {
                    val includePath = childElement.getAttribute("href")
                    if (includePath.isNotBlank()) {
                        val resolvedIncludePath = resolveIncludePath(currentResourcePath, includePath)
                        descriptors += loadServiceDescriptors(resolvedIncludePath, classLoader, visitedResources)
                    }
                }

                childElement.matchesTag(ServiceContainerTag.APPLICATION.tagName) -> {
                    childElement.toServiceDescriptor(ServiceContainerTag.APPLICATION)?.let(descriptors::add)
                }

                childElement.matchesTag(ServiceContainerTag.PROJECT.tagName) -> {
                    childElement.toServiceDescriptor(ServiceContainerTag.PROJECT)?.let(descriptors::add)
                }

                else -> {
                    descriptors += collectServiceDescriptors(
                        rootElement = childElement,
                        currentResourcePath = currentResourcePath,
                        classLoader = classLoader,
                        visitedResources = visitedResources,
                    )
                }
            }
        }

        return descriptors
    }

    private fun loadRootElement(resourcePath: String, classLoader: ClassLoader): Element? {
        return openResource(resourcePath, classLoader)?.use { inputStream ->
            documentBuilderFactory.newDocumentBuilder().parse(inputStream).documentElement
        }
    }

    private fun openResource(resourcePath: String, classLoader: ClassLoader): InputStream? {
        return classLoader.getResource(resourcePath)?.openStream()
    }

    private fun resolveIncludePath(currentResourcePath: String, includePath: String): String {
        val normalizedIncludePath = normalizeResourcePath(includePath)
        if (includePath.startsWith("/")) {
            return normalizedIncludePath
        }

        val currentDirectory = currentResourcePath.substringBeforeLast('/', missingDelimiterValue = "")
        return normalizeResourcePath(
            buildString {
                if (currentDirectory.isNotEmpty()) {
                    append(currentDirectory)
                    append('/')
                }
                append(normalizedIncludePath)
            },
        )
    }

    private fun normalizeResourcePath(resourcePath: String): String {
        val rawSegments = resourcePath.replace('\\', '/').split('/')
        val normalizedSegments = ArrayDeque<String>()

        rawSegments.forEach { segment ->
            when {
                segment.isEmpty() || segment == "." -> Unit
                segment == ".." -> if (normalizedSegments.isNotEmpty()) normalizedSegments.removeLast()
                else -> normalizedSegments.addLast(segment)
            }
        }

        return normalizedSegments.joinToString("/")
    }

    private fun Element.matchesTag(expectedTagName: String): Boolean {
        return localName == expectedTagName ||
            tagName == expectedTagName ||
            tagName.endsWith(":$expectedTagName")
    }

    private fun Element.toServiceDescriptor(containerTag: ServiceContainerTag): ServiceDescriptor? {
        val serviceImplementation = getAttribute("serviceImplementation")
        if (serviceImplementation.isBlank()) {
            return null
        }

        val serviceInterface = getAttribute("serviceInterface").ifBlank { null }
        return ServiceDescriptor(
            containerTag = containerTag,
            serviceInterface = serviceInterface,
            serviceImplementation = serviceImplementation,
        )
    }

    /**
     * 显式包一层泛型入口，避免 `MockComponentManager.registerService`
     * 因类型擦除产生重载歧义。
     */
    private fun <T> MockComponentManager.registerServiceWithInterface(
        interfaceClass: Class<T>,
        implementationClass: Class<T>,
    ) {
        registerService(interfaceClass, implementationClass)
    }

    private val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }
}
