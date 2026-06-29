package org.cangnova.cangjie.extensions

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * Mirrors Kotlin's lightweight extension descriptor model.
 */
abstract class ExtensionPointDescriptor<T : Any>(
    /**
     * IntelliJ 扩展点名称。
     */
    val name: String,
    /**
     * 扩展点接受的扩展实例类型。
     */
    val extensionClass: Class<T>,
)

/**
 * 项目级扩展点描述符，负责在 headless project 环境中按需注册和读取扩展实例。
 */
open class ProjectExtensionDescriptor<T : Any>(
    name: String,
    extensionClass: Class<T>,
) : ExtensionPointDescriptor<T>(name, extensionClass) {
    /**
     * IntelliJ 平台使用的项目扩展点名称对象。
     */
    val extensionPointName: ExtensionPointName<T> = ExtensionPointName.create(name)

    /**
     * 在指定 project 的扩展区中注册该扩展点；已经注册时保持幂等。
     */
    fun registerExtensionPoint(project: Project) {
        if (project.extensionArea.hasExtensionPoint(extensionPointName.name)) return
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            extensionPointName.name,
            extensionClass,
        )
    }

    /**
     * 在指定 project 中注册一个扩展实例。
     */
    fun registerExtension(project: Project, extension: T) {
        registerExtensionPoint(project)
        val extensionPoint = project.extensionArea.getExtensionPoint(extensionPointName)
        if (extension !in extensionPoint.extensions) {
            extensionPoint.registerExtension(extension, project)
        }
    }

    /**
     * 读取指定 project 中该扩展点的全部扩展实例。
     */
    fun getInstances(project: Project): List<T> {
        val projectArea = project.extensionArea
        if (!projectArea.hasExtensionPoint(extensionPointName.name)) return emptyList()
        return projectArea.getExtensionPoint(extensionPointName).extensions.toList()
    }
}
