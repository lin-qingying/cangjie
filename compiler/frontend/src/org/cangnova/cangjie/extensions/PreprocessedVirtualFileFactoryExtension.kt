@file:Suppress("DEPRECATION_ERROR")

package org.cangnova.cangjie.extensions

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

/**
 * 为兼容 Kotlin 预处理文件扩展点而保留的虚拟文件工厂扩展。
 *
 * 扩展可将普通 [VirtualFile] 或轻量文件替换为预处理后的文件视图，前端读取源码时会优先使用替换结果。
 */
@Deprecated("This extension point follows Kotlin compatibility behavior.", level = DeprecationLevel.ERROR)
interface PreprocessedVirtualFileFactoryExtension {
    /**
     * 当前扩展的项目级扩展点描述符。
     */
    companion object : ProjectExtensionDescriptor<PreprocessedVirtualFileFactoryExtension>(
        "org.cangnova.cangjie.preprocessedVirtualFileFactoryExtension",
        PreprocessedVirtualFileFactoryExtension::class.java,
    )

    /**
     * 当前扩展是否仅透传原文件。
     *
     * 透传扩展不会进入 [PreprocessedFileCreator] 的有效扩展列表。
     */
    fun isPassThrough(): Boolean

    /**
     * 为普通虚拟文件创建预处理视图。
     */
    fun createPreprocessedFile(file: VirtualFile?): VirtualFile?

    /**
     * 为轻量虚拟文件创建预处理视图。
     */
    fun createPreprocessedLightFile(file: LightVirtualFile?): LightVirtualFile?
}

/**
 * 基于项目扩展点创建预处理虚拟文件的工具。
 */
class PreprocessedFileCreator(
    /**
     * 用于读取预处理扩展实例的 IntelliJ 项目。
     */
    val project: Project,
) {
    /**
     * 当前项目中真正会参与预处理的扩展实例。
     */
    private val validExtensions: Array<PreprocessedVirtualFileFactoryExtension> by lazy {
        PreprocessedVirtualFileFactoryExtension.getInstances(project)
            .filterNot { it.isPassThrough() }
            .toTypedArray()
    }

    /**
     * 对普通虚拟文件应用第一个可用的预处理扩展。
     */
    fun create(file: VirtualFile): VirtualFile {
        return validExtensions.firstNotNullOfOrNull { it.createPreprocessedFile(file) } ?: file
    }

    /**
     * 对轻量虚拟文件应用第一个可用的预处理扩展。
     */
    fun createLight(file: LightVirtualFile): LightVirtualFile {
        return validExtensions.firstNotNullOfOrNull { it.createPreprocessedLightFile(file) } ?: file
    }
}
