package org.cangnova.cangjie.cfir.deserialization

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import java.nio.file.Path

/**
 * 模块数据提供器抽象。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider`
 */
abstract class ModuleDataProvider : CfirSessionComponent {
    /** 当前提供器可见的全部模块数据。 */
    abstract val allModuleData: Collection<CfirModuleData>

    /** “常规依赖”对应的模块数据（主依赖模块）。 */
    abstract val regularDependenciesModuleData: CfirModuleData

    /** 通过 refinement/depends-on 边引入的依赖模块数据。 */
    open val dependsOnDependenciesModuleData: Collection<CfirModuleData>
        get() = emptyList()

    /** 按库路径定位对应模块数据；找不到时返回 `null`。 */
    abstract fun getModuleData(path: Path?): CfirModuleData?

    /** 反查某个模块数据对应的路径集合；默认无信息。 */
    open fun getModuleDataPaths(moduleData: CfirModuleData): Set<Path>? = null
}

/** 从消费端 session 读取其按库路径归属 depends-on 模块的映射。 */
val CfirSession.cjmpModuleDataProvider: ModuleDataProvider? by
    CfirSession.nullableSessionComponentAccessor()

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

    /** 供反查和最具体来源判定共用的绝对登记路径。 */
    internal open val registeredPaths: Set<Path>? get() = null

    /** 接受所有库路径的过滤器，用于单模块或无需路径隔离的反序列化场景。 */
    object TakeAll : LibraryPathFilter() {
        /** 始终返回 true，表示当前模块数据对任意库路径可见。 */
        override fun accepts(path: Path?): Boolean = true
    }

    /** 不拥有任何二进制路径的模块数据过滤器。 */
    object RejectAll : LibraryPathFilter() {
        /** 始终拒绝路径归属。 */
        override fun accepts(path: Path?): Boolean = false
    }

    /** 接受显式文件或登记目录内的来源，路径比较前会转换为绝对规范路径。 */
    class LibraryList(
        /** 当前模块数据允许绑定的库路径集合。 */
        paths: Set<Path>,
    ) : LibraryPathFilter() {
        override val registeredPaths = paths.mapTo(linkedSetOf()) { it.toAbsolutePath().normalize() }

        /** 对显式文件和登记目录使用同一 Path 边界。 */
        override fun accepts(path: Path?): Boolean {
            val normalizedPath = path?.toAbsolutePath()?.normalize() ?: return false
            return registeredPaths.any(normalizedPath::startsWith)
        }
    }

    /** 接受指定二进制根内的库文件路径。 */
    class LibraryRoots(
        /** 当前模块拥有的文件或目录根。 */
        roots: Collection<Path>,
    ) : LibraryPathFilter() {
        override val registeredPaths = roots.mapTo(linkedSetOf()) { it.toAbsolutePath().normalize() }

        init {
            require(registeredPaths.isNotEmpty()) {
                "LibraryRoots must contain at least one root"
            }
        }

        /** 路径必须位于其中一个模块二进制根内。 */
        override fun accepts(path: Path?): Boolean {
            val normalizedPath = path?.toAbsolutePath()?.normalize() ?: return false
            return registeredPaths.any(normalizedPath::startsWith)
        }
    }

    /** 同时接受精确发布文件和模块二进制根内的文件。 */
    class LibraryPathsAndRoots(
        /** 当前模块精确发布的二进制文件。 */
        paths: Collection<Path>,
        /** 当前模块拥有的目录或文件根。 */
        roots: Collection<Path>,
    ) : LibraryPathFilter() {
        private val normalizedPaths = paths.mapTo(linkedSetOf()) { it.toAbsolutePath().normalize() }
        private val normalizedRoots = roots.mapTo(linkedSetOf()) { it.toAbsolutePath().normalize() }

        override val registeredPaths: Set<Path> = normalizedPaths + normalizedRoots

        /** 路径属于精确文件集合或任一模块根。 */
        override fun accepts(path: Path?): Boolean {
            val normalizedPath = path?.toAbsolutePath()?.normalize() ?: return false
            return normalizedPath in normalizedPaths || normalizedRoots.any {
                normalizedPath == it || normalizedPath.startsWith(it)
            }
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
    /** 通过 refinement/depends-on 边引入的依赖模块数据。 */
    override val dependsOnDependenciesModuleData: Collection<CfirModuleData> = emptyList(),
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

    override fun getModuleData(path: Path?): CfirModuleData? {
        val normalizedPath = path?.toAbsolutePath()?.normalize()
        // CJMP 的父模块带显式依赖顺序。TakeAll 仅用于调用方主动提供的有序映射。
        if (dependsOnDependenciesModuleData.isNotEmpty() || LibraryPathFilter.TakeAll in moduleDataWithFilters.values) {
            return moduleDataWithFilters.entries.firstOrNull { it.value.accepts(normalizedPath) }?.key
        }

        normalizedPath ?: return null
        val matches = moduleDataWithFilters.entries.filter { it.value.accepts(normalizedPath) }
        if (matches.size <= 1) return matches.singleOrNull()?.key
        // 普通库可以嵌套登记目录。最具体的来源决定归属，同一深度的不同 owner 必须拒绝。
        val ownersByDepth = matches.groupBy { (_, filter) ->
            checkNotNull(filter.registeredPaths) { "Overlapping library filters have no explicit root ownership" }
                .filter(normalizedPath::startsWith).maxOf { it.nameCount }
        }
        val owners = ownersByDepth.getValue(ownersByDepth.keys.max())
        check(owners.size == 1) { "Multiple module owners for CJO $normalizedPath: ${owners.map { it.key }}" }
        return owners.single().key
    }

    override fun getModuleDataPaths(moduleData: CfirModuleData): Set<Path>? =
        moduleDataWithFilters[moduleData]?.registeredPaths
}
