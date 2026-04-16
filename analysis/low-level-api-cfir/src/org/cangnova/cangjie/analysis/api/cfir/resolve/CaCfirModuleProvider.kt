package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule

/**
 * low-level use-site 模块提供器。
 *
 * 这一层对齐 Kotlin `LLModuleProvider` 的职责：从 use-site 模块出发，构建当前分析快照可见的模块闭包，
 * 并为后续 session provider、scope cache、diagnostic provider 提供统一的模块遍历入口。
 */
internal class CaCfirModuleProvider(
    val useSiteModule: CaModule,
) {
    val allModules: Set<CaModule> by lazy(LazyThreadSafetyMode.NONE) {
        buildVisibleModuleClosure(useSiteModule)
    }

    private fun buildVisibleModuleClosure(rootModule: CaModule): Set<CaModule> {
        val result = linkedSetOf<CaModule>()

        fun visit(module: CaModule) {
            if (!result.add(module)) return

            module.allDirectDependencies.forEach(::visit)

            if (module is CaDanglingFileModule) {
                module.contextModule?.let(::visit)
            }
            if (module is CaNotUnderContentRootModule) {
                module.originalModule?.let(::visit)
            }
        }

        visit(rootModule)
        return result
    }
}
