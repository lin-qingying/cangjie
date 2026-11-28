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

    /** 接受显式库文件或目录 root 内的真实来源，使用 Path 的目录边界。 */
    class LibraryList(
        /** 当前模块数据允许绑定的库路径集合。 */
        paths: Set<Path>,
    ) : LibraryPathFilter() {
        /** 所有路径在登记时确定为绝对标准路径，供归属选择与反查共同使用。 */
        val libs: Set<Path> = paths.mapTo(linkedSetOf()) { it.toAbsolutePath().normalize() }

        /** `.cjo` 来源可以是显式文件或已登记目录内的文件。 */
        override fun accepts(path: Path?): Boolean {
            val normalizedPath = path?.toAbsolutePath()?.normalize() ?: return false
            return libs.any { normalizedPath.startsWith(it) }
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

    /**
     * 从真实来源选择已登记 owner；不把未知路径交给常规依赖模块。
     *
     * 独立库或 CJMP 文件可以位于另一库目录中，最具体的登记 root 表达真实归属。
     * 同一最具体来源被不同模块同时认领时拒绝，不能按插入顺序选择。
     */
    override fun getModuleData(path: Path?): CfirModuleData? {
        val normalizedPath = path?.toAbsolutePath()?.normalize() ?: return null
        val matches = moduleDataWithFilters.entries.filter { it.value.accepts(normalizedPath) }
        if (matches.isEmpty()) return null
        if (matches.size == 1) return matches.single().key

        val roots = matches.map { (module, filter) ->
            check(filter is LibraryPathFilter.LibraryList) { "Overlapping library filters have no explicit root ownership" }
            module to checkNotNull(filter.libs.filter { normalizedPath.startsWith(it) }.maxByOrNull { it.nameCount })
        }
        val depth = roots.maxOf { it.second.nameCount }
        val owners = roots.filter { it.second.nameCount == depth }
        check(owners.size == 1) { "Multiple module owners for CJO $normalizedPath: ${owners.map { it.second }}" }
        return owners.single().first
    }

    /** 反查的路径与来源匹配使用同一登记事实。 */
    override fun getModuleDataPaths(moduleData: CfirModuleData): Set<Path>? =
        (moduleDataWithFilters[moduleData] as? LibraryPathFilter.LibraryList)?.libs
}
