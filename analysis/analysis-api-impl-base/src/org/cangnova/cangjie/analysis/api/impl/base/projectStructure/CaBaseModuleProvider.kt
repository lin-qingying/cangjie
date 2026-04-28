package org.cangnova.cangjie.analysis.api.impl.base.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModuleProvider

/**
 * 默认 use-site module 选择服务。
 *
 * 对齐 Kotlin `KaBaseModuleProvider`，这里只把 Analysis API 的公开
 * [CaModuleProvider] 委托到平台 project-structure 服务，不直接维护模块图状态。
 */
@CaImplementationDetail
class CaBaseModuleProvider(
    private val project: Project,
) : CaModuleProvider {
    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule =
        CangJieProjectStructureProvider.getModule(project, element, useSiteModule)
}
