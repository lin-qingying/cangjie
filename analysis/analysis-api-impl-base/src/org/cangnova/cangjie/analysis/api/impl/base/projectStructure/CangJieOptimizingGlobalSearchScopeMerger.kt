package org.cangnova.cangjie.analysis.api.impl.base.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaGlobalSearchScopeMerger
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieGlobalSearchScopeMergeStrategy

/**
 * `CangJieOptimizingGlobalSearchScopeMerger` 对位 Kotlin `KotlinOptimizingGlobalSearchScopeMerger`。
 */
internal class CangJieOptimizingGlobalSearchScopeMerger(
    private val project: Project,
) : CaGlobalSearchScopeMerger {
    @OptIn(CaExperimentalApi::class)
    override fun union(scopes: Collection<GlobalSearchScope>): GlobalSearchScope {
        if (scopes.isEmpty()) {
            return GlobalSearchScope.EMPTY_SCOPE
        }

        val providedStrategies = CangJieGlobalSearchScopeMergeStrategy.getMergeStrategies(project)

        val resultingScopes = providedStrategies.fold(scopes) { currentScopes, strategy ->
            currentScopes.applyStrategy(strategy)
        }

        return GlobalSearchScope.union(resultingScopes)
    }

    @OptIn(CaExperimentalApi::class)
    private fun <T : Any> Collection<GlobalSearchScope>.applyStrategy(
        strategy: CangJieGlobalSearchScopeMergeStrategy<T>,
    ): Collection<GlobalSearchScope> {
        val (applicableScopes, restScopes) = partition { strategy.targetType.isInstance(it) }
        if (applicableScopes.isEmpty()) {
            return this
        }

        @Suppress("UNCHECKED_CAST")
        return strategy.uniteScopes(applicableScopes as List<T>) + restScopes
    }
}
