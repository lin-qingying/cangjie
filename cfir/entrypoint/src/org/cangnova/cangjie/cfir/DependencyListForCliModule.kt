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
 * CLI 源模块的依赖模块数据。
 *
 * 仓颉保留 Kotlin 的 regular / depends-on 依赖划分，但有意省略 friend dependencies，
 * 因为当前语言和模块模型不暴露 Kotlin friend-module 语义。
 *
 * @property regularDependencies 普通依赖模块。
 * @property dependsOnDependencies depends-on 依赖模块。
 * @property moduleDataProvider 按库路径查找模块数据的 provider。
 */
class DependencyListForCliModule internal constructor(
    val regularDependencies: List<CfirModuleData>,
    val dependsOnDependencies: List<CfirModuleData>,
    val moduleDataProvider: ModuleDataProvider,
) {
    /**
     * 依赖列表构建入口。
     */
    companion object {
        /**
         * 使用 builder DSL 构建依赖列表。
         */
        inline fun build(init: Builder.() -> Unit = {}): DependencyListForCliModule {
            return Builder().apply(init).build()
        }

        /**
         * 使用默认 regular / depends-on 依赖模块构建依赖列表。
         */
        inline fun build(
            mainModuleName: Name,
            init: Builder.BuilderForDefaultDependenciesModule.() -> Unit = {},
        ): DependencyListForCliModule {
            return build { defaultDependenciesSet(mainModuleName, init) }
        }
    }

    /**
     * CLI 依赖列表 builder。
     */
    class Builder {
        /**
         * 已注册的普通依赖模块。
         */
        private val allRegularDependencies: MutableSet<CfirBinaryDependenciesModuleData> = linkedSetOf()

        /**
         * 已注册的 depends-on 依赖模块。
         */
        private val allDependsOnDependencies: MutableSet<CfirBinaryDependenciesModuleData> = linkedSetOf()

        /**
         * 依赖模块到库路径过滤器输入的映射。
         */
        private val filtersMap: MutableMap<CfirBinaryDependenciesModuleData, MutableSet<Path>> = linkedMapOf()

        /**
         * 注册普通依赖模块及其可见路径。
         */
        fun dependencies(moduleData: CfirBinaryDependenciesModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allRegularDependencies)
        }

        /**
         * 注册 depends-on 依赖模块及其可见路径。
         */
        fun dependsOnDependencies(moduleData: CfirBinaryDependenciesModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allDependsOnDependencies)
        }

        /**
         * 创建并注册默认 regular / depends-on 依赖模块集合。
         */
        inline fun defaultDependenciesSet(
            mainModuleName: Name,
            init: BuilderForDefaultDependenciesModule.() -> Unit,
        ) {
            BuilderForDefaultDependenciesModule(
                regular = createData("<regular dependencies of ${mainModuleName.asString()}>"),
                dependsOn = createData("<dependsOn dependencies of ${mainModuleName.asString()}>"),
            ).apply(init)
        }

        /**
         * 创建二进制依赖模块数据。
         */
        fun createData(name: String): CfirBinaryDependenciesModuleData {
            return CfirBinaryDependenciesModuleData(Name.special(name))
        }

        /**
         * 默认依赖模块 DSL。
         *
         * @property regular 默认普通依赖模块。
         * @property dependsOn 默认 depends-on 依赖模块。
         */
        inner class BuilderForDefaultDependenciesModule(
            val regular: CfirBinaryDependenciesModuleData,
            val dependsOn: CfirBinaryDependenciesModuleData,
        ) {
            init {
                allRegularDependencies += regular
                allDependsOnDependencies += dependsOn
            }

            /**
             * 为默认普通依赖模块注册路径。
             */
            fun dependencies(paths: Collection<String>) {
                dependencies(regular, paths)
            }

            /**
             * 为默认 depends-on 依赖模块注册路径。
             */
            fun dependsOnDependencies(paths: Collection<String>) {
                dependsOnDependencies(dependsOn, paths)
            }
        }

        /**
         * 将依赖模块和路径写入指定目标集合。
         */
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

        /**
         * 构建不可变依赖列表。
         */
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

        /**
         * 过滤出拥有路径过滤器的依赖模块。
         */
        private fun Collection<CfirBinaryDependenciesModuleData>.filterUsedModules(
            pathFiltersMap: Map<CfirModuleData, LibraryPathFilter>,
        ): MutableList<CfirBinaryDependenciesModuleData> {
            return filterTo(mutableListOf()) { it in pathFiltersMap }
        }
    }
}
