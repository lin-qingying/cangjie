package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * `CaResolutionScope` 对位 Kotlin `KaResolutionScope`。
 *
 * 它表示 Analysis API 会话里某个 use-site module 可见、可参与解析的 PSI 范围。
 */
@CaPlatformInterface
abstract class CaResolutionScope : GlobalSearchScope() {
    /**
     * 判断 [element] 是否属于当前解析作用域。
     */
    abstract fun contains(element: PsiElement): Boolean

    /**
     * 底层物理搜索作用域。
     *
     * 仅暴露给 Analysis API 实现与测试使用。
     */
    @CaImplementationDetail
    abstract val underlyingSearchScope: GlobalSearchScope

    @CaPlatformInterface
    companion object {
        fun forModule(useSiteModule: CaModule): CaResolutionScope =
            CaResolutionScopeProvider.getInstance(useSiteModule.project).getResolutionScope(useSiteModule)
    }
}
