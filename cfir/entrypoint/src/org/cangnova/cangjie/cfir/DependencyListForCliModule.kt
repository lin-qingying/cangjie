package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.deserialization.LibraryPathFilter
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.deserialization.MultipleModuleDataProvider
import org.cangnova.cangjie.name.Name
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Describes dependency module data for a CLI source module.
 *
 * Cangjie keeps Kotlin's regular/depends-on split, but intentionally omits
 * friend dependencies because the language/module model in this repository
 * does not expose Kotlin's friend-module semantics.
 */
class DependencyListForCliModule internal constructor(
    val regularDependencies: List<CfirModuleData>,
    val dependsOnDependencies: List<CfirModuleData>,
    val moduleDataProvider: ModuleDataProvider,
) {
    companion object {
        inline fun build(init: Builder.() -> Unit = {}): DependencyListForCliModule {
            return Builder().apply(init).build()
        }

        inline fun build(
            mainModuleName: Name,
            init: Builder.BuilderForDefaultDependenciesModule.() -> Unit = {},
        ): DependencyListForCliModule {
            return build { defaultDependenciesSet(mainModuleName, init) }
        }
    }

    class Builder {
        private val allRegularDependencies: MutableSet<CfirBinaryDependenciesModuleData> = linkedSetOf()
        private val allDependsOnDependencies: MutableSet<CfirBinaryDependenciesModuleData> = linkedSetOf()
        private val filtersMap: MutableMap<CfirBinaryDependenciesModuleData, MutableSet<Path>> = linkedMapOf()

        fun dependencies(moduleData: CfirBinaryDependenciesModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allRegularDependencies)
        }

        fun dependsOnDependencies(moduleData: CfirBinaryDependenciesModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allDependsOnDependencies)
        }

        inline fun defaultDependenciesSet(
            mainModuleName: Name,
            init: BuilderForDefaultDependenciesModule.() -> Unit,
        ) {
            BuilderForDefaultDependenciesModule(
                regular = createData("<regular dependencies of ${mainModuleName.asString()}>"),
                dependsOn = createData("<dependsOn dependencies of ${mainModuleName.asString()}>"),
            ).apply(init)
        }

        fun createData(name: String): CfirBinaryDependenciesModuleData {
            return CfirBinaryDependenciesModuleData(Name.special(name))
        }

        inner class BuilderForDefaultDependenciesModule(
            val regular: CfirBinaryDependenciesModuleData,
            val dependsOn: CfirBinaryDependenciesModuleData,
        ) {
            init {
                allRegularDependencies += regular
                allDependsOnDependencies += dependsOn
            }

            fun dependencies(paths: Collection<String>) {
                dependencies(regular, paths)
            }

            fun dependsOnDependencies(paths: Collection<String>) {
                dependsOnDependencies(dependsOn, paths)
            }
        }

        private fun dependencies(
            moduleData: CfirBinaryDependenciesModuleData,
            paths: Collection<String>,
            destination: MutableSet<CfirBinaryDependenciesModuleData>,
        ) {
            destination += moduleData
            if (paths.isEmpty()) return

            val filterSet = filtersMap.getOrPut(moduleData) { linkedSetOf() }
            paths.mapTo(filterSet) { Paths.get(it) }
        }

        fun build(): DependencyListForCliModule {
            val pathFiltersMap = filtersMap
                .filterValues { it.isNotEmpty() }
                .mapValuesTo(linkedMapOf<CfirModuleData, LibraryPathFilter>()) { (_, paths) ->
                    LibraryPathFilter.LibraryList(paths)
                }

            val regularDependencies = allRegularDependencies.filterUsedModules(pathFiltersMap)
            val dependsOnDependencies = allDependsOnDependencies.filterUsedModules(pathFiltersMap)

            allRegularDependencies.singleOrNull()?.let { regularModule ->
                pathFiltersMap.putIfAbsent(regularModule, LibraryPathFilter.TakeAll)
                if (regularModule !in regularDependencies) {
                    regularDependencies += regularModule
                }
            }

            require(regularDependencies.isNotEmpty()) {
                "DependencyListForCliModule requires at least one regular dependency module"
            }

            val moduleDataProvider = MultipleModuleDataProvider(pathFiltersMap, regularDependencies.first())
            return DependencyListForCliModule(
                regularDependencies = regularDependencies,
                dependsOnDependencies = dependsOnDependencies,
                moduleDataProvider = moduleDataProvider,
            )
        }

        private fun Collection<CfirBinaryDependenciesModuleData>.filterUsedModules(
            pathFiltersMap: Map<CfirModuleData, LibraryPathFilter>,
        ): MutableList<CfirBinaryDependenciesModuleData> {
            return filterTo(mutableListOf()) { it in pathFiltersMap }
        }
    }
}
