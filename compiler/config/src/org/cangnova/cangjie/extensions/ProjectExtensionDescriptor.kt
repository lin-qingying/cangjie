package org.cangnova.cangjie.extensions

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * Mirrors Kotlin's lightweight extension descriptor model.
 */
abstract class ExtensionPointDescriptor<T : Any>(
    val name: String,
    val extensionClass: Class<T>,
)

open class ProjectExtensionDescriptor<T : Any>(
    name: String,
    extensionClass: Class<T>,
) : ExtensionPointDescriptor<T>(name, extensionClass) {
    val extensionPointName: ExtensionPointName<T> = ExtensionPointName.create(name)

    fun registerExtensionPoint(project: Project) {
        if (project.extensionArea.hasExtensionPoint(extensionPointName.name)) return
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            extensionPointName.name,
            extensionClass,
        )
    }

    fun registerExtension(project: Project, extension: T) {
        registerExtensionPoint(project)
        val extensionPoint = project.extensionArea.getExtensionPoint(extensionPointName)
        if (extension !in extensionPoint.extensions) {
            extensionPoint.registerExtension(extension, project)
        }
    }

    fun getInstances(project: Project): List<T> {
        val projectArea = project.extensionArea
        if (!projectArea.hasExtensionPoint(extensionPointName.name)) return emptyList()
        return projectArea.getExtensionPoint(extensionPointName).extensions.toList()
    }
}
