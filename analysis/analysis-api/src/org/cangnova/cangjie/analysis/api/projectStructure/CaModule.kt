package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

interface CaModule {
    val directRegularDependencies: List<CaModule>
        get() = emptyList()

    val directDependsOnDependencies: List<CaModule>
        get() = emptyList()

    val transitiveDependsOnDependencies: List<CaModule>
        get() = collectTransitiveDependsOnDependencies()

    val directFriendDependencies: List<CaModule>
        get() = emptyList()

    val allDirectDependencies: List<CaModule>
        get() = buildList {
            addAll(directRegularDependencies)
            addAll(directDependsOnDependencies)
            addAll(directFriendDependencies)
        }.distinct()

    val baseContentScope: GlobalSearchScope

    val contentScope: GlobalSearchScope
        get() = baseContentScope

    val project: Project

    val moduleDescription: String
        get() = this::class.simpleName ?: "CaModule"

    val stableModuleName: String?
        get() = null

    val targetPlatform: CaTargetPlatform
        get() = CaTargetPlatform.DEFAULT

    val isResolvable: Boolean
        get() = true
}

private fun CaModule.collectTransitiveDependsOnDependencies(): List<CaModule> {
    val result = linkedSetOf<CaModule>()

    fun visit(module: CaModule) {
        module.directDependsOnDependencies.forEach { dependency ->
            if (result.add(dependency)) {
                visit(dependency)
            }
        }
    }

    visit(this)
    return result.toList()
}
