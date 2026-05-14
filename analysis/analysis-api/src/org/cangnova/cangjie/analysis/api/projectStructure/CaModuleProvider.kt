package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail

/**
 * Analysis API 的 use-site module 选择服务。
 *
 * 该接口对齐 Kotlin `KaModuleProvider`:它只负责在给定 use-site module 语境下,
 * 把一个 PSI 元素映射为可解析的 [CaModule]。
 *
 * 平台侧完整模块图快照由 `platform.projectStructure.CaModuleProvider` 承载,
 * 两者职责不能混用:本接口面向 IDE/工具的"在哪个上下文模块里解析这个元素",
 * 平台侧 provider 面向引擎的"项目里有哪些模块、它们的依赖图是什么"。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaModuleProvider {
    /**
     * 在 [useSiteModule] 上下文中,返回 [element] 对应的可解析模块。
     *
     * 返回的 [CaModule] 一定满足 [CaModule.isResolvable]。
     *
     * ### Use-site 模块
     *
     * Use-site module 指的是发起本次 [getModule] 调用的模块上下文。
     * 当调用方明确知道使用现场对应的模块时,应传入该模块以消歧;
     * 这对同一个 PSI 可能属于多个模块(典型如 outsider 文件)的场景尤为重要。
     */
    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule

    companion object {
        /**
         * 获取项目级别的 [CaModuleProvider] 服务。
         */
        fun getInstance(project: Project): CaModuleProvider = project.service()

        /**
         * 便捷入口:在 [project] 内为 [element] 计算 use-site 模块。
         */
        fun getModule(project: Project, element: PsiElement, useSiteModule: CaModule?): CaModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
