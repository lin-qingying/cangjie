package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaEngineService

/**
 * `CaGlobalSearchScopeMerger` 对位 Kotlin `KaGlobalSearchScopeMerger`。
 *
 * 它按注册的 [CangJieGlobalSearchScopeMergeStrategy] 合并多个 [GlobalSearchScope]，
 * 为内容作用域、解析作用域和符号 provider 统一提供可扁平化的 union 入口。
 */
@CaPlatformInterface
interface CaGlobalSearchScopeMerger : CaEngineService {
    /**
     * 创建表示 [scopes] 并集的 [GlobalSearchScope]。
     */
    fun union(scopes: Collection<GlobalSearchScope>): GlobalSearchScope

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CaGlobalSearchScopeMerger = project.service()
    }
}
