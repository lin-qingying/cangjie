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
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.test.services.TestServices
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.isRegularFile

/**
 * compiled `.cjo` analysis:stubs 测试环境工具。
 *
 * 该对象封装 builtins project structure 安装、stdlib fixture 定位与系统属性切换。
 */
internal object CjoCompiledTestEnvironment {
    /**
     * 在测试项目中安装只包含 builtins module 的 standalone project structure。
     */
    fun installBuiltinsProjectStructure(project: Project): CaBuiltinsModule {
        val builtinsModule = object : CaModuleBase(), CaBuiltinsModule {
            override val project = project
            override val builtinsName: String = "<stubs-test-builtins>"
            override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
            override val isResolvable: Boolean
                get() = true
            override val baseContentScope: GlobalSearchScope
                get() = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)
        }
        val projectStructure = CaStandaloneProjectStructure(listOf(builtinsModule))
        project.getService(CaStandalonePlatformState::class.java).install(projectStructure)
        return builtinsModule
    }

    /**
     * 通过 decompiled binary index 查找指定 builtins 包对应的 `.cjo` 文件。
     */
    fun findBuiltinsBinaryFile(
        project: Project,
        builtinsModule: CaBuiltinsModule,
        packageFqName: FqName,
    ) = project.getService(CaDecompiledBinaryIndex::class.java)
        .findBinaryFile(builtinsModule, packageFqName)

    /**
     * 将指定相对路径的 stdlib fixture 复制到临时目录，并以该目录执行测试动作。
     */
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

    /**
     * 使用仓库内完整 stdlib fixture 根执行测试动作。
     */
    fun <T> withFullStdlibFixture(action: (stdlibRoot: Path) -> T): T {
        return withStdlibFixtureProperty(locateStdlibFixtureRoot(), action)
    }

    /**
     * 定位当前 Gradle 仓库根目录。
     */
    fun locateRepositoryRoot(): Path {
        val start = Paths.get("").toAbsolutePath().normalize()
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
    }

    /**
     * 定位仓库内 stdlib `.cjo` fixture 根目录。
     */
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

    /**
     * 临时设置 `cangjie.stdlib.module` 系统属性并执行测试动作。
     */
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

/**
 * compiled `.cjo` stub 测试使用的 Analysis API 服务注册器。
 */
internal object CjoCompiledStubsTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * 测试需要注册的 analysis API 插件 XML 列表。
     */
    private val pluginXmls = listOf(
        "META-INF/analysis-api/cangjie-low-level-api-cfir.xml",
        "META-INF/analysis-api/cangjie-analysis-stubs.xml",
    )

    /**
     * 注册 application 级服务，包括 low-level/stubs 插件服务和 decompiled 测试服务。
     */
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        pluginXmls.forEach { pluginXmlPath ->
            PluginStructureProvider.registerApplicationServices(application, pluginXmlPath)
        }
        CaAnalysisApiDecompiledTestServiceRegistrar.registerApplicationServices(application)
    }

    /**
     * 注册 project 级服务，包括 standalone platform state、插件服务和 decompiled 测试服务。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.registerService(CaStandalonePlatformState::class.java, CaStandalonePlatformState::class.java)
        pluginXmls.forEach { pluginXmlPath ->
            PluginStructureProvider.registerProjectServices(project, pluginXmlPath)
        }
        CaAnalysisApiDecompiledTestServiceRegistrar.registerProjectServices(project)
    }
}
