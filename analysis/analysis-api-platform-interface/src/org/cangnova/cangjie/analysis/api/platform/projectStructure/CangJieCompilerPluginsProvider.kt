package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaOptionalPlatformComponent
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.extensions.ExtensionPointDescriptor

/**
 * `CangJieCompilerPluginsProvider` 对位 Kotlin `KotlinCompilerPluginsProvider`。
 *
 * 平台如果没有实现该组件，则 Analysis API 视为当前工程没有注册编译器插件。
 */
@CaPlatformInterface
interface CangJieCompilerPluginsProvider : CaOptionalPlatformComponent {
    /**
     * Analysis API 关心的编译器插件类型。
     */
    @CaPlatformInterface
    enum class CompilerPluginType {
        ASSIGNMENT,
    }

    /**
     * 返回指定模块中注册到扩展点的插件扩展实例。
     */
    fun <T : Any> getRegisteredExtensions(module: CaModule, extensionType: ExtensionPointDescriptor<T>): List<T>

    /**
     * 判断指定模块是否注册了给定类型的编译器插件。
     */
    fun isPluginOfTypeRegistered(module: CaModule, pluginType: CompilerPluginType): Boolean

    @CaPlatformInterface
    companion object {
        /**
         * 获取可选的项目级编译器插件 provider 服务。
         */
        fun getInstance(project: Project): CangJieCompilerPluginsProvider? = project.serviceOrNull()
    }
}
