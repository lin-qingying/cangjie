package org.cangnova.cangjie.analysis.api.projectStructure

/**
 * 库的兜底依赖模块。
 *
 * - 当一个 [CaLibraryModule] 或其源码模块的真实依赖未知时,
 *   引擎需要一个占位模块代替"该库实际依赖的其他库";
 * - 兜底依赖会覆盖项目中除 [dependentLibrary] 自身以外的所有库内容,
 *   解析时虽不完全精确,但在多数情况下足够选中正确符号;
 * - 该模块**不是**可解析模块,不能直接作为 `analyze()` 的 use-site module,
 *   也不会由 [CaModuleProvider.getModule] 返回。
 *
 * 对齐 Kotlin Analysis API 的 `KaLibraryFallbackDependenciesModule`。
 */
interface CaLibraryFallbackDependenciesModule : CaModule {
    /**
     * 依赖该兜底模块的库的稳定标识(通常为库名)。
     */
    val dependencyOwnerName: String

    /**
     * 人类可读的模块描述。
     */
    override val moduleDescription: String
        get() = "Fallback dependencies of $dependencyOwnerName"
}
