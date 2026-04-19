package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaOptionalPlatformComponent
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * `CangJieModuleInformationProvider` 对位 Kotlin `KotlinModuleInformationProvider`。
 *
 * 该服务承载不适合直接放进 [CaModule] 主 API 的附加模块信息。
 */
@CaPlatformInterface
interface CangJieModuleInformationProvider : CaOptionalPlatformComponent {
    /**
     * 返回模块是否为空；无法判断时返回 `null`。
     */
    fun isEmpty(module: CaModule): Boolean?

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CangJieModuleInformationProvider? = project.serviceOrNull()
    }
}
