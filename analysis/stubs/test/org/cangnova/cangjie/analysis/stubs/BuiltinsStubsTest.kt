package org.cangnova.cangjie.analysis.stubs

import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.util.requireIsInstance
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText

class BuiltinsStubsTest : AbstractAnalysisApiBasedTest() {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> = listOf(
        CjoCompiledStubsTestServiceRegistrar,
    )

    @Test
    fun builtinsStubsAndDecompiledText() {
        CjoCompiledTestEnvironment.withFullStdlibFixture {
            val testDataFile = CjoCompiledTestEnvironment.locateRepositoryRoot()
                .resolve("analysis")
                .resolve("stubs")
                .resolve("testData")
                .resolve("builtins")
                .resolve("test.cj")
            runTest(testDataFile.toString()) { testServices ->
                val project = testServices.cjTestModuleStructure.project
                CjoCompiledTestEnvironment.installBuiltinsProjectStructure(project)
                val psiManager = PsiManager.getInstance(project)
                val targetFileName = System.getProperty("cangjie.builtins.test.file")
                val builtinFiles = BuiltinsVirtualFileProvider.getInstance()
                    .getBuiltinVirtualFiles()
                    .sortedBy { it.path }
                    .filter { virtualFile -> targetFileName == null || virtualFile.name == targetFileName }
                    .mapNotNull { virtualFile -> psiManager.findFile(virtualFile) }

                assertTrue(builtinFiles.isNotEmpty(), "Builtins provider must expose `.cjo` files")

                builtinFiles.forEach { builtinFile ->
                    println("BuiltinsStubsTest: ${builtinFile.name} compute")
                    requireIsInstance<CjDecompiledFile>(builtinFile)
                    val fileStub = CjoCompiledStubsTestEngine.compute(builtinFile)
                    assertMatchesGolden(
                        actual = CjoCompiledStubsTestEngine.render(fileStub),
                        expectedFile = goldenFile(builtinFile.name, ".stubs.txt"),
                    )
                    println("BuiltinsStubsTest: ${builtinFile.name} text")
                    assertMatchesGolden(
                        actual = requireNotNull(builtinFile.text) {
                            "${builtinFile.name} unexpectedly doesn't have decompiled text"
                        },
                        expectedFile = goldenFile(builtinFile.name, ".decompiled.text.cj"),
                    )
                    println("BuiltinsStubsTest: ${builtinFile.name} validate")
                    CjoCompiledStubsTestEngine.validate(builtinFile, fileStub)
                }
            }
        }
    }

    private fun goldenFile(fileName: String, suffix: String): Path {
        return CjoCompiledTestEnvironment.locateRepositoryRoot()
            .resolve("analysis")
            .resolve("stubs")
            .resolve("testData")
            .resolve("builtins")
            .resolve("$fileName$suffix")
    }

    private fun assertMatchesGolden(actual: String, expectedFile: Path) {
        val normalizedActual = actual.normalizeLineSeparators().trimEnd()
        if (System.getProperty("update.test.data")?.toBooleanStrictOrNull() == true) {
            Files.createDirectories(expectedFile.parent)
            Files.writeString(expectedFile, normalizedActual + "\n")
            return
        }

        require(expectedFile.exists()) {
            "Missing golden file: $expectedFile\nRun with -Dupdate.test.data=true to create it."
        }
        val expected = expectedFile.readText().normalizeLineSeparators().trimEnd()
        org.junit.jupiter.api.Assertions.assertEquals(expected, normalizedActual, "Mismatch in ${expectedFile.name}")
    }

    private fun String.normalizeLineSeparators(): String = replace("\r\n", "\n")
}
