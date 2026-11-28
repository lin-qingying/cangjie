package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.deserialization.LibraryPathFilter
import org.cangnova.cangjie.cfir.deserialization.MultipleModuleDataProvider
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.nio.file.Path

/** CLI builder 决定依赖角色顺序；通用 provider 按调用方传入的过滤器顺序判定真实路径归属。 */
class DependencyListForCliModuleTest {
    @Test
    fun `depends on exact paths precede regular exact paths regardless of registration order`() {
        val sharedPath = libraryPath("shared.cjo")
        for (regularFirst in listOf(true, false)) {
            val regular = moduleData("regular")
            val common = moduleData("common")
            val dependencies = DependencyListForCliModule.build {
                if (regularFirst) {
                    dependencies(regular, listOf(sharedPath.toString()))
                    dependsOnDependencies(common, listOf(sharedPath.toString()))
                } else {
                    dependsOnDependencies(common, listOf(sharedPath.toString()))
                    dependencies(regular, listOf(sharedPath.toString()))
                }
            }

            assertSame(common, dependencies.moduleDataProvider.getModuleData(sharedPath), "regularFirst=$regularFirst")
            assertEquals(listOf(common, regular), dependencies.moduleDataProvider.allModuleData.toList())
            assertEquals(listOf(common), dependencies.dependsOnDependencies)
            assertEquals(listOf(regular), dependencies.regularDependencies)
            assertNull(dependencies.moduleDataProvider.getModuleData(libraryPath("unknown.cjo")))
        }
    }

    @Test
    fun `overlapping common filters retain registration order within their dependency role`() {
        val sharedPath = libraryPath("shared.cjo")
        val firstOnlyPath = libraryPath("first-only.cjo")
        val secondOnlyPath = libraryPath("second-only.cjo")
        val regularPath = libraryPath("regular.cjo")
        for (reverseCommonOrder in listOf(false, true)) {
            val first = moduleData("first common")
            val second = moduleData("second common")
            val regular = moduleData("regular")
            val commonOrder = if (reverseCommonOrder) listOf(second, first) else listOf(first, second)
            val dependencies = DependencyListForCliModule.build {
                dependencies(regular, listOf(regularPath.toString()))
                for (common in commonOrder) {
                    val exclusivePath = if (common === first) firstOnlyPath else secondOnlyPath
                    dependsOnDependencies(common, listOf(sharedPath.toString(), exclusivePath.toString()))
                }
            }

            assertSame(commonOrder.first(), dependencies.moduleDataProvider.getModuleData(sharedPath))
            assertSame(first, dependencies.moduleDataProvider.getModuleData(firstOnlyPath))
            assertSame(second, dependencies.moduleDataProvider.getModuleData(secondOnlyPath))
            assertSame(regular, dependencies.moduleDataProvider.getModuleData(regularPath))
            assertEquals(commonOrder, dependencies.dependsOnDependencies)
            assertEquals(commonOrder + regular, dependencies.moduleDataProvider.allModuleData.toList())
        }
    }

    @Test
    fun `default regular module accepts remaining paths after explicit common paths`() {
        val firstCommonPath = libraryPath("common-first.cjo")
        val secondCommonPath = libraryPath("common-second.cjo")
        val dependencies = DependencyListForCliModule.build(Name.identifier("consumer")) {
            dependsOnDependencies(listOf(firstCommonPath.toString(), secondCommonPath.toString()))
        }
        val commonModules = dependencies.dependsOnDependencies
        val regular = dependencies.regularDependencies.single()
        val provider = dependencies.moduleDataProvider

        assertEquals(2, commonModules.size)
        assertSame(commonModules[0], provider.getModuleData(firstCommonPath))
        assertSame(commonModules[1], provider.getModuleData(secondCommonPath))
        assertSame(regular, provider.getModuleData(libraryPath("ordinary.cjo")))
        assertSame(regular, provider.getModuleData(Path.of("other-libraries", "unknown.cjo")))
        assertSame(regular, provider.getModuleData(null))
        assertEquals(commonModules + regular, provider.allModuleData.toList())
    }

    @Test
    fun `direct provider respects caller supplied filter order including take all`() {
        val commonPath = libraryPath("common.cjo")
        val regular = moduleData("regular")
        val common = moduleData("common")
        val commonFilter = LibraryPathFilter.LibraryList(setOf(commonPath))
        for (takeAllFirst in listOf(true, false)) {
            val filters = if (takeAllFirst) {
                linkedMapOf<CfirModuleData, LibraryPathFilter>(regular to LibraryPathFilter.TakeAll, common to commonFilter)
            } else {
                linkedMapOf<CfirModuleData, LibraryPathFilter>(common to commonFilter, regular to LibraryPathFilter.TakeAll)
            }
            val provider = MultipleModuleDataProvider(
                moduleDataWithFilters = filters,
                regularDependenciesModuleData = regular,
                dependsOnDependenciesModuleData = listOf(common),
            )

            assertSame(if (takeAllFirst) regular else common, provider.getModuleData(commonPath), "takeAllFirst=$takeAllFirst")
            assertSame(regular, provider.getModuleData(libraryPath("ordinary.cjo")))
            assertSame(regular, provider.getModuleData(null))
            assertEquals(filters.keys.toList(), provider.allModuleData.toList())
        }
    }

    @Test
    fun `explicit regular filter leaves unknown paths unmatched and common files are exact`() {
        val commonPath = libraryPath("common.cjo")
        val regularPath = libraryPath("regular.cjo")
        val dependencies = DependencyListForCliModule.build(Name.identifier("consumer")) {
            dependencies(listOf(regularPath.toString()))
            dependsOnDependencies(listOf(commonPath.toString()))
        }
        val regular = dependencies.regularDependencies.single()
        val common = dependencies.dependsOnDependencies.single()
        val provider = dependencies.moduleDataProvider

        assertSame(common, provider.getModuleData(commonPath))
        assertSame(regular, provider.getModuleData(regularPath))
        assertSame(common, provider.getModuleData(commonPath.parent.resolve(".").resolve(commonPath.fileName)))
        assertNull(provider.getModuleData(commonPath.parent.resolve("same-directory-other.cjo")))
        assertNull(provider.getModuleData(Path.of("other-libraries", "unknown.cjo")))
        assertNull(provider.getModuleData(null))
        assertEquals(listOf(common, regular), provider.allModuleData.toList())
    }

    private fun moduleData(name: String): CfirBinaryDependenciesModuleData =
        CfirBinaryDependenciesModuleData(Name.special("<$name>"))

    private fun libraryPath(name: String): Path = Path.of("dependency-list-test", name).toAbsolutePath().normalize()
}
