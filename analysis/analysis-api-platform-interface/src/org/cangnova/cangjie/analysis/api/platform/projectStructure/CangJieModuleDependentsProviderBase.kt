package org.cangnova.cangjie.analysis.api.platform.projectStructure

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * `CangJieModuleDependentsProviderBase` 对位 Kotlin `KotlinModuleDependentsProviderBase`。
 *
 * 该基类统一承载传递依赖方的递归遍历与去重语义，避免具体平台实现重复展开这段契约逻辑。
 */
@CaPlatformInterface
abstract class CangJieModuleDependentsProviderBase : CangJieModuleDependentsProvider {
    /**
     * 通过直接依赖方递归计算传递依赖方。
     */
    override fun getTransitiveDependents(module: CaModule): Set<CaModule> = computeTransitiveDependents(module)

    /**
     * 递归遍历依赖方图并排除查询模块自身。
     */
    protected fun computeTransitiveDependents(module: CaModule): Set<CaModule> = buildSet {
        fun visit(currentModule: CaModule) {
            if (currentModule in this) return
            add(currentModule)
            getDirectDependents(currentModule).forEach(::visit)
        }

        visit(module)

        // 契约要求返回值中不包含查询入参本身。
        remove(module)
    }
}
