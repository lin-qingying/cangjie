package org.cangnova.cangjie.analysis.api.impl.base.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaContentScopeProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaContentScopeRefiner
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaGlobalSearchScopeMerger
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * `CaModule.contentScope` 的默认引擎实现。
 */
internal class CaBaseContentScopeProvider : CaContentScopeProvider {
    /**
     * 返回经过平台 refiner 扩展和限制后的模块内容作用域。
     */
    @OptIn(CaPlatformInterface::class)
    override fun getRefinedContentScope(module: CaModule): GlobalSearchScope {
        val baseContentScope = module.baseContentScope

        val refiners = CaContentScopeRefiner.getRefiners(module.project).ifEmpty {
            return baseContentScope
        }

        val enlargementScopes = mutableListOf(baseContentScope)
        val restrictionScopes = mutableListOf<GlobalSearchScope>()

        refiners.forEach { refiner ->
            enlargementScopes.addAll(
                refiner.getEnlargementScopes(module).filterNot(GlobalSearchScope::isEmptyScope),
            )

            val refinerRestrictionScopes = refiner.getRestrictionScopes(module)
            if (refinerRestrictionScopes.any(GlobalSearchScope::isEmptyScope)) {
                return GlobalSearchScope.EMPTY_SCOPE
            }
            restrictionScopes.addAll(refinerRestrictionScopes)
        }

        return mergeScopes(module, enlargementScopes, restrictionScopes)
    }

    /**
     * 合并扩展 scope，并按顺序应用限制 scope。
     */
    private fun mergeScopes(
        module: CaModule,
        enlargementScopes: MutableList<GlobalSearchScope>,
        restrictionScopes: MutableList<GlobalSearchScope>,
    ): GlobalSearchScope {
        val scopeMerger = CaGlobalSearchScopeMerger.getInstance(module.project)
        val mergedEnlargementScope = scopeMerger.union(enlargementScopes)
        if (restrictionScopes.isEmpty()) {
            return mergedEnlargementScope
        }

        return restrictionScopes.fold(mergedEnlargementScope) { resultScope, scope ->
            resultScope.intersectWith(scope)
        }
    }
}
