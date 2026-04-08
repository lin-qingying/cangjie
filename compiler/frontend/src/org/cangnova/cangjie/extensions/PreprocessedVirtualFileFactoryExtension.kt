@file:Suppress("DEPRECATION_ERROR")

package org.cangnova.cangjie.extensions

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

@Deprecated("This extension point follows Kotlin compatibility behavior.", level = DeprecationLevel.ERROR)
interface PreprocessedVirtualFileFactoryExtension {
    companion object : ProjectExtensionDescriptor<PreprocessedVirtualFileFactoryExtension>(
        "org.cangnova.cangjie.preprocessedVirtualFileFactoryExtension",
        PreprocessedVirtualFileFactoryExtension::class.java,
    )

    fun isPassThrough(): Boolean

    fun createPreprocessedFile(file: VirtualFile?): VirtualFile?

    fun createPreprocessedLightFile(file: LightVirtualFile?): LightVirtualFile?
}

class PreprocessedFileCreator(val project: Project) {
    private val validExtensions: Array<PreprocessedVirtualFileFactoryExtension> by lazy {
        PreprocessedVirtualFileFactoryExtension.getInstances(project)
            .filterNot { it.isPassThrough() }
            .toTypedArray()
    }

    fun create(file: VirtualFile): VirtualFile {
        return validExtensions.firstNotNullOfOrNull { it.createPreprocessedFile(file) } ?: file
    }

    fun createLight(file: LightVirtualFile): LightVirtualFile {
        return validExtensions.firstNotNullOfOrNull { it.createPreprocessedLightFile(file) } ?: file
    }
}
