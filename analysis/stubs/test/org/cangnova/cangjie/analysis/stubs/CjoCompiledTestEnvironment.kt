@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.stubs

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleBase
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandalonePlatformState
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneProjectStructure
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.test.services.TestServices
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.isRegularFile

internal object CjoCompiledTestEnvironment {
    fun installBuiltinsProjectStructure(project: Project): CaBuiltinsModule {
        val builtinsModule = object : CaModuleBase(), CaBuiltinsModule {
            override val project = project
            override val builtinsName: String = "<stubs-test-builtins>"
            override val isResolvable: Boolean
                get() = true
            override val baseContentScope: GlobalSearchScope
                get() = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)
        }
        val projectStructure = CaStandaloneProjectStructure(listOf(builtinsModule))
        project.getService(CaStandalonePlatformState::class.java).install(projectStructure)
        return builtinsModule
    }

    fun findBuiltinsBinaryFile(
        project: Project,
        builtinsModule: CaBuiltinsModule,
        packageFqName: FqName,
    ) = project.getService(CaDecompiledBinaryIndex::class.java)
        .findBinaryFile(builtinsModule, packageFqName)

    fun <T> withSlimStdlibFixture(
        vararg relativePaths: String,
        action: (stdlibRoot: Path) -> T,
    ): T {
        val sourceRoot = locateStdlibFixtureRoot()
        val tempRoot = Files.createTempDirectory("cangjie-stubs-stdlib")
        relativePaths.forEach { relativePath ->
            val sourceFile = sourceRoot.resolve(relativePath)
            require(sourceFile.isRegularFile()) { "Missing stdlib fixture file: $sourceFile" }
            val targetFile = tempRoot.resolve(relativePath)
            Files.createDirectories(targetFile.parent)
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }
        return withStdlibFixtureProperty(tempRoot, action)
    }

    fun <T> withFullStdlibFixture(action: (stdlibRoot: Path) -> T): T {
        return withStdlibFixtureProperty(locateStdlibFixtureRoot(), action)
    }

    fun locateRepositoryRoot(): Path {
        val start = Paths.get("").toAbsolutePath().normalize()
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
    }

    private fun locateStdlibFixtureRoot(): Path {
        val fixtureRoot = locateRepositoryRoot()
            .resolve("cfir")
            .resolve("cfir-serialization")
            .resolve("testResources")
            .resolve("cjo-sdk")
            .resolve("windows_x86_64_cjnative")
        require(fixtureRoot.resolve("std.cjo").isRegularFile()) {
            "Cannot locate stdlib fixture root under $fixtureRoot"
        }
        return fixtureRoot
    }

    private fun <T> withStdlibFixtureProperty(stdlibRoot: Path, action: (stdlibRoot: Path) -> T): T {
        val oldValue = System.getProperty("cangjie.stdlib.module")
        try {
            System.setProperty("cangjie.stdlib.module", stdlibRoot.toString())
            return action(stdlibRoot)
        } finally {
            if (oldValue == null) {
                System.clearProperty("cangjie.stdlib.module")
            } else {
                System.setProperty("cangjie.stdlib.module", oldValue)
            }
        }
    }
}

internal object CjoCompiledStubsTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    private val pluginXmls = listOf(
        "META-INF/analysis-api/cangjie-low-level-api-cfir.xml",
        "META-INF/analysis-api/cangjie-analysis-stubs.xml",
    )

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        pluginXmls.forEach { pluginXmlPath ->
            PluginStructureProvider.registerApplicationServices(application, pluginXmlPath)
        }
        CaAnalysisApiDecompiledTestServiceRegistrar.registerApplicationServices(application)
    }

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.registerService(CaStandalonePlatformState::class.java, CaStandalonePlatformState::class.java)
        pluginXmls.forEach { pluginXmlPath ->
            PluginStructureProvider.registerProjectServices(project, pluginXmlPath)
        }
        CaAnalysisApiDecompiledTestServiceRegistrar.registerProjectServices(project)
    }
}
