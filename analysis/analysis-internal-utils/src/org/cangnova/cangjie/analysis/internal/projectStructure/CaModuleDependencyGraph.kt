package org.cangnova.cangjie.analysis.internal.projectStructure

import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * Analysis 模块依赖图工具。
 *
 * 该工具统一处理模块可达闭包的遍历规则，
 * 让 standalone、测试框架与平台缓存逻辑都围绕同一套“直接依赖协议”工作，
 * 不再分别手写 regular / dependsOn / friend 三类边的遍历。
 */
object CaModuleDependencyGraph {
    /**
     * 从单个入口模块收集包含自身在内的可达模块闭包。
     */
    fun collectReachableModules(root: CaModule): List<CaModule> {
        return collectReachableModules(listOf(root))
    }

    /**
     * 从多个入口模块统一收集可达闭包。
     *
     * 这里显式把“多入口模块图并集”建模为依赖图工具本身的能力，
     * 避免 standalone / 测试 / 平台调用方继续各自手写
     * `linkedSet + forEach(root.collectReachableModules())`。
     */
    fun collectReachableModules(roots: Collection<CaModule>): List<CaModule> {
        val visited = linkedSetOf<CaModule>()
        roots.forEach { root -> visit(root, visited) }
        return visited.toList()
    }

    /**
     * 深度优先遍历模块依赖边，并保持首次访问顺序。
     */
    private fun visit(module: CaModule, visited: LinkedHashSet<CaModule>) {
        if (!visited.add(module)) return

        module.allDirectDependencies.forEach { visit(it, visited) }
    }
}

/**
 * 从当前模块收集包含自身在内的依赖闭包。
 */
fun CaModule.collectReachableModules(): List<CaModule> =
    CaModuleDependencyGraph.collectReachableModules(this)

/**
 * 从多个入口模块收集合并后的依赖闭包。
 */
fun Collection<CaModule>.collectReachableModules(): List<CaModule> =
    CaModuleDependencyGraph.collectReachableModules(this)
