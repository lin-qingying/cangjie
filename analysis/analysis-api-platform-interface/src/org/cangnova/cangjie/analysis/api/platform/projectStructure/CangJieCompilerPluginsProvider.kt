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
    @CaPlatformInterface
    enum class CompilerPluginType {
        ASSIGNMENT,
    }

    fun <T : Any> getRegisteredExtensions(module: CaModule, extensionType: ExtensionPointDescriptor<T>): List<T>

    fun isPluginOfTypeRegistered(module: CaModule, pluginType: CompilerPluginType): Boolean

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CangJieCompilerPluginsProvider? = project.serviceOrNull()
    }
}
