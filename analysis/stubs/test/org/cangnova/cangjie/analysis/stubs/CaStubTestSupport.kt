@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.stubs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleBase
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandalonePlatformState
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneProjectStructure
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.StandardCopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

internal object CaStubTestSupport {
    fun withEnvironment(
        testName: String,
        action: (environment: CangJieCoreEnvironment) -> Unit,
    ) {
        val disposable = Disposer.newDisposable(testName)
        try {
            val environment = CangJieCoreEnvironment.createForTests(disposable)
            action(environment)
        } finally {
            val application = ApplicationManager.getApplication()
            if (application != null) {
                application.runWriteAction {
                    Disposer.dispose(disposable)
                }
            } else {
                Disposer.dispose(disposable)
            }
        }
    }

    fun createSourceFile(
        environment: CangJieCoreEnvironment,
        fileName: String,
        text: String,
    ): CjFile {
        return PsiFileFactory.getInstance(environment.project).createFileFromText(
            fileName,
            CangJieFileType.INSTANCE,
            text,
        ) as CjFile
    }

    fun withRegisteredStubAndDecompilerServices(
        environment: CangJieCoreEnvironment,
        action: () -> Unit,
    ) {
        val application = environment.applicationEnvironment.application as MockApplication
        val project = environment.project as MockProject
        pluginXmlPaths.forEach { pluginXmlPath ->
            PluginStructureProvider.registerApplicationServices(application, pluginXmlPath)
            PluginStructureProvider.registerProjectServices(project, pluginXmlPath)
        }
        action()
    }

    fun installBuiltinsProjectStructure(
        environment: CangJieCoreEnvironment,
    ): CaBuiltinsModule {
        val builtinsModule = SimpleBuiltinsModule(environment.project)
        val projectStructure = CaStandaloneProjectStructure(listOf(builtinsModule))
        environment.project.getService(CaStandalonePlatformState::class.java).install(projectStructure)
        return builtinsModule
    }

    fun <T> withStdlibFixtureProperty(action: (stdlibRoot: Path) -> T): T {
        return withStdlibFixtureProperty(locateStdlibFixtureRoot(), action)
    }

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

    fun findBuiltinsBinaryFile(
        environment: CangJieCoreEnvironment,
        builtinsModule: CaBuiltinsModule,
        packageFqName: FqName,
    ) = environment.project.getService(CaDecompiledBinaryIndex::class.java)
        .findBinaryFile(builtinsModule, packageFqName)

    fun assertMatchesGolden(
        actual: String,
        expectedFile: Path,
    ) {
        val normalizedActual = actual.normalizeLineSeparators().trimEnd()
        if (shouldUpdateGoldenFiles()) {
            Files.createDirectories(expectedFile.parent)
            Files.writeString(expectedFile, normalizedActual + "\n")
            return
        }

        require(expectedFile.exists()) {
            "Missing golden file: $expectedFile\nRun with -Dupdate.test.data=true to create it."
        }
        val expected = expectedFile.readText().normalizeLineSeparators().trimEnd()
        assertEquals(expected, normalizedActual)
    }

    fun locateRepositoryRoot(): Path {
        val start = Paths.get("").toAbsolutePath().normalize()
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
    }

    fun locateStdlibFixtureRoot(): Path {
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

    fun renderSummary(summary: CaStubFileSummary): String {
        return buildString {
            appendLine("fileKey=${summary.fileKey.stableFileKey()}")
            appendLine("kind=${summary.stubKind ?: "<missing>"}")
            appendLine("package=${summary.packageFqName?.asString() ?: "<missing>"}")
            appendLine("topLevelClassifiers=${summary.topLevelClassifierNames.map(Name::asString).sorted()}")
            appendLine("topLevelCallables=${summary.topLevelCallableNames.map(Name::asString).sorted()}")
            appendLine("classMembers=")
            if (summary.classMemberNames.isEmpty()) {
                append("  <none>")
            } else {
                summary.classMemberNames.toSortedMap(compareBy(ClassId::asString)).forEach { (classId, names) ->
                    appendLine("  ${classId.asFqNameString()}=${names.map(Name::asString).sorted()}")
                }
            }
        }.trimEnd()
    }

    fun renderSnapshot(snapshot: CaStubSnapshot): String {
        return buildString {
            appendLine("modificationCount=${snapshot.modificationCount}")
            appendLine("files=${snapshot.fileSummaries.keys.map { it.stableFileKey() }.sorted()}")
            appendLine("packages.classifiers=")
            if (snapshot.packageClassifierNames.isEmpty()) {
                appendLine("  <none>")
            } else {
                snapshot.packageClassifierNames.toSortedMap(compareBy(FqName::asString)).forEach { (fqName, names) ->
                    appendLine("  ${fqName.asString()}=${names.map(Name::asString).sorted()}")
                }
            }
            appendLine("packages.callables=")
            if (snapshot.packageCallableNames.isEmpty()) {
                appendLine("  <none>")
            } else {
                snapshot.packageCallableNames.toSortedMap(compareBy(FqName::asString)).forEach { (fqName, names) ->
                    appendLine("  ${fqName.asString()}=${names.map(Name::asString).sorted()}")
                }
            }
            appendLine("classMembers=")
            if (snapshot.classMemberNames.isEmpty()) {
                append("  <none>")
            } else {
                snapshot.classMemberNames.toSortedMap(compareBy(ClassId::asString)).forEach { (classId, names) ->
                    appendLine("  ${classId.asFqNameString()}=${names.map(Name::asString).sorted()}")
                }
            }
        }.trimEnd()
    }

    private fun shouldUpdateGoldenFiles(): Boolean {
        return System.getProperty("update.test.data")?.toBooleanStrictOrNull() == true
    }

    private fun String.normalizeLineSeparators(): String {
        return replace("\r\n", "\n")
    }

    private fun String.stableFileKey(): String {
        return substringAfterLast('/').substringAfterLast('\\')
    }

    private val pluginXmlPaths = listOf(
        "META-INF/analysis-api/cangjie-analysis-api-standalone.xml",
        "META-INF/analysis-api/cangjie-analysis-decompiled.xml",
        "META-INF/analysis-api/cangjie-analysis-stubs.xml",
    )

    private const val stdlibModulePropertyName = "cangjie.stdlib.module"

    private fun <T> withStdlibFixtureProperty(
        stdlibRoot: Path,
        action: (stdlibRoot: Path) -> T,
    ): T {
        val oldValue = System.getProperty(stdlibModulePropertyName)
        try {
            System.setProperty(stdlibModulePropertyName, stdlibRoot.toString())
            return action(stdlibRoot)
        } finally {
            if (oldValue == null) {
                System.clearProperty(stdlibModulePropertyName)
            } else {
                System.setProperty(stdlibModulePropertyName, oldValue)
            }
        }
    }

    /**
     * `analysis:stubs` 的 compiled 测试只需要一个最小 builtins module 视图，
     * 不应该反向依赖 analysis-test-framework 里的测试实现。
     */
    private class SimpleBuiltinsModule(
        override val project: com.intellij.openapi.project.Project,
    ) : CaModuleBase(), CaBuiltinsModule {
        override val builtinsName: String = "<stubs-test-builtins>"

        override val isResolvable: Boolean
            get() = true

        override val baseContentScope: GlobalSearchScope
            get() = CaBuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)
    }
}
