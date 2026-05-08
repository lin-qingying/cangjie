package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.openapi.application.ApplicationManager
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOriginKind
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lightDeclarations.documentation
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjNamedDeclaration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.isRegularFile

/**
 * 锁定 builtins/decompiled light declaration 的文档边界。
 *
 * 这里验证两件事：
 * 1. builtins use-site 下，light declaration provider 能返回 decompiled 视图；
 * 2. 当前 decompiled 边界没有真实 CDoc 时，文档恢复稳定返回 null。
 */
class AnalysisApiBuiltinsLightDeclarationDocumentationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/lightDeclarationDocsBuiltins",
) {
    private val stdlibModulePropertyName = "cangjie.stdlib.module"

    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun builtinsLightDeclarationDocs(mainFile: CjFile, mainModule: CjTestModule) {
        withSlimStdlibFixture("std.cjo", "std/std.core.cjo", "std/std.objectpool.cjo") { stdlibRoot ->
            withStdlibFixtureProperty(stdlibRoot) {
                val decompiledFile = findBuiltinsObjectPool(mainFile, mainModule)
                assertNotNull(decompiledFile, "builtins decompiled PSI 应可恢复 `std.objectpool`")

                val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
                val allDeclarations = ApplicationManager.getApplication().runWriteAction<List<CaLightDeclaration>> {
                    provider.getLightDeclarations(decompiledFile!!, mainModule.caModule)
                }
                val declaration = allDeclarations.firstOrNull { lightDeclaration ->
                    lightDeclaration.origin.kind == CaLightDeclarationOriginKind.DECOMPILED_PSI
                }
                assertNotNull(declaration, "builtins light declaration provider 应返回 decompiled 视图")
                assertTrue(declaration!!.origin.containingFile?.isCompiled == true)
                assertFalse(declaration.origin.description.isBlank())

                analyzeForTest(mainFile) {
                    assertEquals(null, documentation(declaration))
                }

            }
        }
    }

    @Test
    fun builtinsDecompiledText(mainFile: CjFile, mainModule: CjTestModule) {
        withSlimStdlibFixture("std.cjo", "std/std.core.cjo", "std/std.objectpool.cjo") { stdlibRoot ->
            withStdlibFixtureProperty(stdlibRoot) {
                val decompiledFile = findBuiltinsObjectPool(mainFile, mainModule)
                assertNotNull(decompiledFile)
                assertTrue(decompiledFile!!.text.contains("ObjectPool"))
                assertTrue(decompiledFile.text.length < 200_000, "std.objectpool decompiled text 不应异常膨胀")
            }
        }
    }

    @Test
    fun builtinsDecompiledTopLevelDeclarations(mainFile: CjFile, mainModule: CjTestModule) {
        withSlimStdlibFixture("std.cjo", "std/std.core.cjo", "std/std.objectpool.cjo") { stdlibRoot ->
            withStdlibFixtureProperty(stdlibRoot) {
                val decompiledFile = findBuiltinsObjectPool(mainFile, mainModule)
                assertNotNull(decompiledFile)
                val actualDeclarations = decompiledFile!!.declarations.filterIsInstance<CjNamedDeclaration>().mapNotNull { declaration -> declaration.name }
                assertEquals(
                    listOf("ObjectPool"),
                    actualDeclarations,
                )
            }
        }
    }

    private fun findBuiltinsObjectPool(mainFile: CjFile, mainModule: CjTestModule): CjFile? {
        return ApplicationManager.getApplication().runWriteAction<CjFile?> {
            mainFile.project.getService(CaDecompiledPsiProvider::class.java)
                .findBuiltinsDecompiledFile(FqName("std.objectpool"))
        }
    }

    private fun locateStdlibFixtureRoot(): Path {
        val repoRoot = locateRepositoryRoot(Paths.get("").toAbsolutePath().normalize())
        val fixtureRoot = repoRoot
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

    private fun locateRepositoryRoot(start: Path): Path {
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
    }

    private fun <T> withSlimStdlibFixture(
        vararg relativePaths: String,
        action: (stdlibRoot: Path) -> T,
    ): T {
        val sourceRoot = locateStdlibFixtureRoot()
        val tempRoot = Files.createTempDirectory("cangjie-analysis-api-stdlib")
        relativePaths.forEach { relativePath ->
            val sourceFile = sourceRoot.resolve(relativePath)
            require(sourceFile.isRegularFile()) { "Missing stdlib fixture file: $sourceFile" }
            val targetFile = tempRoot.resolve(relativePath)
            Files.createDirectories(targetFile.parent)
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }
        return action(tempRoot)
    }

    private fun <T> withStdlibFixtureProperty(stdlibRoot: Path, action: () -> T): T {
        val oldValue = System.getProperty(stdlibModulePropertyName)
        try {
            System.setProperty(stdlibModulePropertyName, stdlibRoot.toString())
            return action()
        } finally {
            if (oldValue == null) {
                System.clearProperty(stdlibModulePropertyName)
            } else {
                System.setProperty(stdlibModulePropertyName, oldValue)
            }
        }
    }
}
