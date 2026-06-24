package org.cangnova.cangjie.cfir.deserialization

import org.cangnova.cangjie.cfir.common.CfirModuleData
import java.nio.file.Path

/**
 * 模块数据提供器抽象。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider`
 */
abstract class ModuleDataProvider {
    /** 当前提供器可见的全部模块数据。 */
    abstract val allModuleData: Collection<CfirModuleData>

    /** “常规依赖”对应的模块数据（主依赖模块）。 */
    abstract val regularDependenciesModuleData: CfirModuleData

    /** 按库路径定位对应模块数据；找不到时返回 `null`。 */
    abstract fun getModuleData(path: Path?): CfirModuleData?

    /** 反查某个模块数据对应的路径集合；默认无信息。 */
    open fun getModuleDataPaths(moduleData: CfirModuleData): Set<Path>? = null
}

/**
 * 单模块提供器。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider`
 */
class SingleModuleDataProvider(
    /** 唯一模块数据。 */
    private val moduleData: CfirModuleData,
) : ModuleDataProvider() {
    /** 全部模块数据即单元素集合。 */
    override val allModuleData: Collection<CfirModuleData>
        get() = listOf(moduleData)

    /** 常规依赖模块即该唯一模块。 */
    override val regularDependenciesModuleData: CfirModuleData
        get() = moduleData

    /** 任意路径都映射到该唯一模块。 */
    override fun getModuleData(path: Path?): CfirModuleData = moduleData
}

/**
 * 库路径过滤器抽象。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.fir.deserialization.AbstractFirDeserializedSymbolProvider.LibraryPathFilter`
 */
abstract class LibraryPathFilter {
    /** 判定是否接受给定路径。 */
    abstract fun accepts(path: Path?): Boolean

    /** 接受所有库路径的过滤器，用于单模块或无需路径隔离的反序列化场景。 */
    object TakeAll : LibraryPathFilter() {
        /** 始终返回 true，表示当前模块数据对任意库路径可见。 */
        override fun accepts(path: Path?): Boolean = true
    }

    /** 仅接受给定路径集合中的库文件，路径比较前会进行标准化。 */
    class LibraryList(
        /** 当前模块数据允许绑定的库路径集合。 */
        private val paths: Set<Path>,
    ) : LibraryPathFilter() {
        /** 标准化输入路径并检查其是否属于 [paths]。 */
        override fun accepts(path: Path?): Boolean {
            val normalizedPath = path?.normalize() ?: return false
            return normalizedPath in paths
        }
    }
}

/**
 * 多模块提供器。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.fir.deserialization.MultipleModuleDataProvider`
 */
class MultipleModuleDataProvider(
    /** 模块数据到路径过滤器的映射。 */
    private val moduleDataWithFilters: Map<CfirModuleData, LibraryPathFilter>,
    /** 常规依赖模块数据。 */
    override val regularDependenciesModuleData: CfirModuleData,
) : ModuleDataProvider() {

    init {
        require(moduleDataWithFilters.isNotEmpty()) {
            "ModuleDataProvider must contain at least one module data"
        }
        require(regularDependenciesModuleData in moduleDataWithFilters) {
            "moduleDataWithFilters should contain regularDependenciesModuleData"
        }
    }

    /** 全部模块数据来自映射键集合。 */
    override val allModuleData: Collection<CfirModuleData>
        get() = moduleDataWithFilters.keys

    /** 先标准化路径，再按过滤器顺序匹配。 */
    override fun getModuleData(path: Path?): CfirModuleData? {
        val normalizedPath = path?.normalize()
        for ((moduleData, filter) in moduleDataWithFilters) {
            if (filter.accepts(normalizedPath)) {
                return moduleData
            }
        }
        return null
    }
}
