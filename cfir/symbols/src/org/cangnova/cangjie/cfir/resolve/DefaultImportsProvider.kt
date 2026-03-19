package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.name.FqName

abstract class DefaultImportsProvider {
    private val defaultImports: List<ImportPath> = listOf(
        "std.core.*",

    ).map { ImportPath.fromString(it) }

    abstract val platformSpecificDefaultImports: List<ImportPath>
    open val defaultLowPriorityImports: List<ImportPath> get() = emptyList()

    open val excludedImports: List<FqName> get() = emptyList()

    fun getDefaultImports(includeLowPriorityImports: Boolean): List<ImportPath> {
        return buildList {
            addAll(defaultImports)
            addAll(platformSpecificDefaultImports)
            if (includeLowPriorityImports) {
                addAll(defaultLowPriorityImports)
            }
        }
    }

    class Composed(val providers: List<DefaultImportsProvider>) : DefaultImportsProvider() {
        override val platformSpecificDefaultImports: List<ImportPath> by lazy {
            providers.map { it.platformSpecificDefaultImports }
                .reduce<_, Collection<ImportPath>> { acc, list -> acc.intersect(list) }
                .toList()
        }

        override val defaultLowPriorityImports: List<ImportPath> by lazy {
            providers.map { it.defaultLowPriorityImports }
                .reduce { acc, list -> acc + list }
                .distinct()
        }

        override val excludedImports: List<FqName> by lazy {
            providers.map { it.excludedImports }
                .reduce { acc, list -> acc + list }
                .distinct()
        }
    }
}
