@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * Analysis API 测试宿主的 project-structure 服务。
 *
 * 测试模式下不再直接把 provider 实例塞进 Pico 容器，
 * 而是与其它测试平台服务一样统一委托给 [CaTestPlatformState]。
 * 这样 project service 的装配边界保持一致，后续只需要安装一次测试模块图。
 */
class CaTestProjectStructureProvider(
    private val project: Project,
) : CangJieProjectStructureProvider {
    private val state: CaTestPlatformState
        get() = project.getService(CaTestPlatformState::class.java)

    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        return state.getModule(element, useSiteModule)
    }

    override fun getImplementingModules(module: CaModule): List<CaModule> {
        return state.snapshot.allModules.filter { module in it.directDependsOnDependencies }
    }

    override val globalLanguageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettings.DEFAULT
}
