package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.platform.TargetPlatform

/**
 * Analysis API 视角下的模块抽象。
 *
 * - 表示一个可被分析的代码单元:源码模块([CaSourceModule])、二进制库
 *   ([CaLibraryModule])、库源码([CaLibrarySourceModule])、游离文件
 *   ([CaDanglingFileModule])、内置模块([CaBuiltinsModule]) 等;
 * - 由 [CaModuleProvider] 在工程结构内查找,由 Analysis API 平台
 *   (IntelliJ / Standalone / LSP 等) 提供具体实现;
 * - 决定符号可见性、依赖图、内容根,是 `analyze()` 的 use-site 单元;
 * - 也是缓存失效与模块级修改追踪的基本单元。
 *
 * 对齐 Kotlin Analysis API 的 `KaModule`。
 */
interface CaModule {
    /**
     * 当前模块直接声明的常规依赖。
     *
     * 该列表**不**做传递闭包,也不包含当前模块自身。
     */
    val directRegularDependencies: List<CaModule>
        get() = emptyList()

    /**
     * 当前模块直接声明的 `dependsOn` 依赖。
     *
     * `dependsOn` 表达多平台层级:当前模块可为依赖模块提供 `actual`,
     * 并能够看到依赖模块的内部符号。本身传递,但该列表不做闭包。
     */
    val directDependsOnDependencies: List<CaModule>
        get() = emptyList()

    /**
     * 直接和间接的所有 `dependsOn` 依赖,按拓扑顺序排列(近的在前)。
     *
     * 不包含当前模块自身。默认实现会基于 [directDependsOnDependencies] 收集闭包。
     */
    val transitiveDependsOnDependencies: List<CaModule>
        get() = collectTransitiveDependsOnDependencies()

    /**
     * 当前模块直接声明的 friend 依赖。
     *
     * Friend 依赖允许当前模块访问目标模块的 `internal` 符号。
     */
    val directFriendDependencies: List<CaModule>
        get() = emptyList()

    /**
     * 当前模块所有的直接依赖(regular + dependsOn + friend),已去重。
     */
    val allDirectDependencies: List<CaModule>
        get() = buildList {
            addAll(directRegularDependencies)
            addAll(directDependsOnDependencies)
            addAll(directFriendDependencies)
        }.distinct()

    /**
     * 模块未经 content scope refiner 加工的基础内容范围。
     *
     * 注意:[baseContentScope] 不代表模块当前真实的内容范围,
     * 应使用 [contentScope] 取最终值。
     */
    val baseContentScope: GlobalSearchScope

    /**
     * 模块的最终内容范围。
     *
     * 一般在 [baseContentScope] 基础上,通过 content scope refiner 扩展点
     * 懒构建得到。
     */
    val contentScope: GlobalSearchScope

    /**
     * 模块所属的 IntelliJ [Project]。所有依赖模块必须属于同一个 [Project]。
     */
    val project: Project

    /**
     * 人类可读的模块描述,用于诊断与调试。
     */
    val moduleDescription: String
        get() = this::class.simpleName ?: "CaModule"

    /**
     * 用于 `internal` 可见性 mangling 的稳定二进制名称。
     *
     * 不是所有模块都需要稳定名称;默认返回 `null`。
     */
    val stableModuleName: String?
        get() = null

    /**
     * 模块对应的仓颉目标平台身份，例如 `cjnative` / `cjvm`。
     *
     * 对齐 Kotlin Analysis API 的 `KaModule.targetPlatform`，这里只表达高层编译目标，
     * 不混入 IDE / standalone / LSP 等宿主环境语义。
     */
    val targetPlatform: TargetPlatform

    /**
     * 当前模块是否可作为 `analyze()` 的 use-site module。
     *
     * 部分模块(例如 [CaLibraryFallbackDependenciesModule])只作为依赖参与,
     * 不允许直接被解析。
     */
    val isResolvable: Boolean
        get() = true
}

/**
 * 递归收集所有传递性的 `dependsOn` 依赖,保持插入顺序去重。
 */
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
