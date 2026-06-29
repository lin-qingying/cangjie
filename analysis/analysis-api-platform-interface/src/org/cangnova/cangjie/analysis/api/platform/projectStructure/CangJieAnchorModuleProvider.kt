package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaOptionalPlatformComponent
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule

/**
 * `CangJieAnchorModuleProvider` 对位 Kotlin `KotlinAnchorModuleProvider`。
 *
 * anchor module 用于把库模块需要可见、但并非以库文件存在的源码模块显式挂入依赖图。
 */
@CaPlatformInterface
interface CangJieAnchorModuleProvider : CaOptionalPlatformComponent {
    /**
     * 返回库模块对应的源码 anchor module。
     */
    fun getAnchorModule(libraryModule: CaLibraryModule): CaSourceModule?

    /**
     * 返回所有 anchor module。
     */
    fun getAllAnchorModules(): Collection<CaSourceModule>

    /**
     * 在已计算时返回所有 anchor module，否则返回 null。
     */
    fun getAllAnchorModulesIfComputed(): Collection<CaSourceModule>?

    @CaPlatformInterface
    companion object {
        /**
         * 获取可选的项目级 anchor module provider 服务。
         */
        fun getInstance(project: Project): CangJieAnchorModuleProvider? = project.serviceOrNull()
    }
}
