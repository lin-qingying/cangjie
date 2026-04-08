package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationKind
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOriginKind
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lightDeclarations.documentation
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.name.FqName
import java.nio.file.Path
import java.nio.file.Paths
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

    override val configurator: AnalysisApiTestConfigurator =
        CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                frontend = FrontendKind.Cfir,
                moduleKind = TestModuleKind.Builtins,
                analysisSessionMode = AnalysisSessionMode.Normal,
                analysisApiMode = AnalysisApiMode.Standalone,
            ),
        )

    @Test
    fun builtinsLightDeclarationDocs(mainFile: CjFile, mainModule: CjTestModule) {
        withStdlibFixtureProperty(locateStdlibFixtureRoot()) {
            val decompiledFile = mainFile.project.getService(CaDecompiledPsiProvider::class.java)
                .findDecompiledFile(mainModule.caModule as org.cangnova.cangjie.analysis.api.CaBuiltinsModule, FqName("std.core"))
            assertNotNull(decompiledFile, "builtins decompiled PSI 应可恢复 `std.core`")

            val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
            val declaration = provider.getLightDeclarations(decompiledFile!!, mainModule.caModule)
                .firstOrNull { lightDeclaration ->
                    lightDeclaration.origin.kind == CaLightDeclarationOriginKind.DECOMPILED_PSI &&
                        lightDeclaration.kind != CaLightDeclarationKind.PACKAGE
                }

            assertNotNull(declaration, "builtins light declaration provider 应返回 decompiled 视图")
            assertTrue(declaration!!.origin.containingFile?.isCompiled == true)
            assertFalse(declaration.origin.description.isBlank())

            analyzeForTest(mainFile) {
                assertEquals(null, documentation(declaration))
            }
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
