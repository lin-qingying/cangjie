package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail

/**
 * Analysis API 的 use-site module 选择服务。
 *
 * 该接口对齐 Kotlin `KaModuleProvider`：它只负责在给定 use-site module 语境下，
 * 把一个 PSI 元素映射为可解析的 [CaModule]。平台侧完整模块图快照由
 * `platform.projectStructure.CaModuleProvider` 承载，两者职责不能混用。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaModuleProvider {
    /**
     * Returns a [CaModule] for a given [element] in the context of the [useSiteModule].
     *
     * The resulting [CaModule] is guaranteed to be [resolvable][CaModule.isResolvable].
     *
     * ### Use-site Modules
     *
     * The use-site module is the [CaModule] from which [getModule] is called.
     * If this module is already known, it should be passed as the [useSiteModule] to
     * disambiguate elements that may be associated with multiple modules.
     */
    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule

    companion object {
        fun getInstance(project: Project): CaModuleProvider = project.service()

        fun getModule(project: Project, element: PsiElement, useSiteModule: CaModule?): CaModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
