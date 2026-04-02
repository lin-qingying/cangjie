package org.cangnova.cangjie.analysis.api

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings

/**
 * Analysis API 视角下的模块抽象。
 *
 * 对齐 Kotlin `KaModule` 的职责，这里不再把模块简化为“只有名称的容器”，
 * 而是显式描述模块依赖关系、内容范围以及 use-site session 的边界。
 */
interface CaModule {
    /**
     * 当前模块的常规依赖。
     */
    val directRegularDependencies: List<CaModule>
        get() = emptyList()

    /**
     * 当前模块的 `dependsOn` 依赖。
     */
    val directDependsOnDependencies: List<CaModule>
        get() = emptyList()

    /**
     * 当前模块的传递 `dependsOn` 依赖。
     */
    val transitiveDependsOnDependencies: List<CaModule>
        get() = collectTransitiveDependsOnDependencies()

    /**
     * 当前模块的 friend 依赖。
     */
    val directFriendDependencies: List<CaModule>
        get() = emptyList()

    /**
     * 模块的基础内容范围。
     */
    val baseContentScope: GlobalSearchScope

    /**
     * 模块的实际内容范围。
     *
     * 当前仓颉实现尚未引入 scope refiner，因此默认直接复用 [baseContentScope]。
     */
    val contentScope: GlobalSearchScope
        get() = baseContentScope

    /**
     * 模块所属 IntelliJ Project。
     */
    val project: Project

    /**
     * 调试与报错使用的模块描述。
     */
    val moduleDescription: String
        get() = this::class.simpleName ?: "CaModule"

    /**
     * 稳定模块名。
     *
     * 对 Kotlin Analysis API 来说，该值会影响 `internal` 可见性与符号身份。
     * 当前仓颉仅测试模块提供稳定名称，平台实现后应由各平台模块给出更准确的结果。
     */
    val stableModuleName: String?
        get() = null
}

/**
 * 源码模块。
 *
 * 对齐 Kotlin `KaSourceModule`。Analysis API 的大多数 use-site 分析都从源码模块进入。
 */
interface CaSourceModule : CaModule {
    val name: String

    val languageVersionSettings: LanguageVersionSettings

    /**
     * PSI 视角下的源码根。
     *
     * 在当前测试实现中，源码根直接由测试 PSI 文件本身承担。
     */
    val psiRoots: List<PsiFileSystemItem>
        get() = emptyList()

    override val moduleDescription: String
        get() = "Sources of $name"

    override val stableModuleName: String?
        get() = name
}

/**
 * 不在内容根下的模块。
 *
 * 对齐 Kotlin `KaNotUnderContentRootModule`，为测试文件、临时文件和代码片段预留语义位置。
 */
interface CaNotUnderContentRootModule : CaModule {
    val name: String

    override val moduleDescription: String
        get() = "Not-under-content-root module $name"
}

private fun CaModule.collectTransitiveDependsOnDependencies(): List<CaModule> {
    val result = linkedSetOf<CaModule>()

    fun visit(module: CaModule) {
        for (dependency in module.directDependsOnDependencies) {
            if (result.add(dependency)) {
                visit(dependency)
            }
        }
    }

    visit(this)
    return result.toList()
}
