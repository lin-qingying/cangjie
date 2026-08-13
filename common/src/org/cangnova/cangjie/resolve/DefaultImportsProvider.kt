package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.name.FqName

/**
 * 默认导入集合提供器。
 */
abstract class DefaultImportsProvider {
    /**
     * 所有平台共享的默认导入。
     */
    open val defaultImports: List<ImportPath> = listOf(
        "std.core.*",

    ).map { ImportPath.fromString(it) }

    /**
     * 平台专属默认导入。
     */
    abstract val platformSpecificDefaultImports: List<ImportPath>
    /**
     * 低优先级默认导入。
     */
    open val defaultLowPriorityImports: List<ImportPath> get() = emptyList()

    /**
     * 默认导入中需要排除的完整限定名。
     */
    open val excludedImports: List<FqName> get() = emptyList()

    /**
     * 按配置组合公共、平台和可选低优先级默认导入。
     */
    fun getDefaultImports(includeLowPriorityImports: Boolean): List<ImportPath> {
        return buildList {
            addAll(defaultImports)
            addAll(platformSpecificDefaultImports)
            if (includeLowPriorityImports) {
                addAll(defaultLowPriorityImports)
            }
        }
    }

    /**
     * 多个默认导入提供器的组合视图。
     */
    class Composed(
        /**
         * 被组合的平台默认导入提供器列表。
         */
        val providers: List<DefaultImportsProvider>,
    ) : DefaultImportsProvider() {
        /**
         * 多平台共同拥有的平台专属导入交集。
         */
        override val platformSpecificDefaultImports: List<ImportPath> by lazy {
            providers.map { it.platformSpecificDefaultImports }
                .reduce<_, Collection<ImportPath>> { acc, list -> acc.intersect(list) }
                .toList()
        }

        /**
         * 所有提供器低优先级导入的去重并集。
         */
        override val defaultLowPriorityImports: List<ImportPath> by lazy {
            providers.map { it.defaultLowPriorityImports }
                .reduce { acc, list -> acc + list }
                .distinct()
        }

        /**
         * 所有提供器排除导入的去重并集。
         */
        override val excludedImports: List<FqName> by lazy {
            providers.map { it.excludedImports }
                .reduce { acc, list -> acc + list }
                .distinct()
        }
    }
}
