@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProviderBase
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CaNotUnderContentRootModuleImpl

/**
 * Analysis API 测试宿主的 project-structure 服务。
 *
 * 测试模式下不再直接把 provider 实例塞进 Pico 容器，
 * 而是与其它测试平台服务一样统一委托给 [CaTestPlatformState]。
 * 这样 project service 的装配边界保持一致，后续只需要安装一次测试模块图。
 */
class CaTestProjectStructureProvider(
    /**
     * 当前测试平台服务绑定的 project。
     */
    private val project: Project,
) : CangJieProjectStructureProviderBase() {
    /**
     * 当前测试平台状态。
     */
    private val state: CaTestPlatformState
        get() = project.getService(CaTestPlatformState::class.java)

    /**
     * 解析 PSI 元素所属模块，优先处理 code fragment 等特殊模块。
     */
    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        element.containingFile?.let { containingFile ->
            computeSpecialModule(containingFile)?.let { return it }
        }

        return state.getModule(element, useSiteModule)
    }

    /**
     * 返回直接 depends-on 指定模块的测试模块集合。
     */
    override fun getImplementingModules(module: CaModule): List<CaModule> {
        return state.snapshot.allModules.filter { module in it.directDependsOnDependencies }
    }

    /**
     * 测试环境默认使用全局默认语言版本设置。
     */
    override val globalLanguageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    /**
     * 创建不属于内容 root 的临时兜底模块。
     */
    override fun getNotUnderContentRootModule(project: Project): CaNotUnderContentRootModule {
        return CaNotUnderContentRootModuleImpl(
            name = "unnamed-outside-content-root",
            originalModule = null,
            project = project,
            scopeRoots = emptyList(),
        )
    }
}
