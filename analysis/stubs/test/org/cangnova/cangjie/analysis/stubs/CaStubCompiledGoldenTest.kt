package org.cangnova.cangjie.analysis.stubs

import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * compiled stub 的 golden 基线。
 *
 * 这里固定选取体量较小的 `std.objectpool`，避免把测试变成大规模 builtins 扫描；
 * 同时它仍然走真实 `.cjo -> compiled PSI -> stub summary` 链路。
 */
class CaStubCompiledGoldenTest : AbstractAnalysisApiBasedTest() {
    /**
     * 使用 standalone CFIR 分析 API 测试配置执行 compiled stub golden 流程。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 注册 compiled `.cjo` 测试所需服务。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> = listOf(
        CjoCompiledStubsTestServiceRegistrar,
    )

    /**
     * 验证 `std.objectpool` 的 compiled stub summary 与 golden 文件一致。
     */
    @Test
    fun builtinsObjectPoolSummary() {
        CjoCompiledTestEnvironment.withSlimStdlibFixture(
            "std.cjo",
            "std/std.core.cjo",
            "std/std.objectpool.cjo",
        ) {
            val testDataFile = CjoCompiledTestEnvironment.locateRepositoryRoot()
                .resolve("analysis")
                .resolve("stubs")
                .resolve("testData")
                .resolve("compiled")
                .resolve("std.objectpool.cj")
            runTest(testDataFile.toString()) { testServices ->
                val project = testServices.cjTestModuleStructure.project
                val builtinsModule = CjoCompiledTestEnvironment.installBuiltinsProjectStructure(project)
                val packageFqName = FqName("std.objectpool")
                val binaryFile = requireNotNull(
                    CjoCompiledTestEnvironment.findBuiltinsBinaryFile(project, builtinsModule, packageFqName),
                ) {
                    "builtins binary index should resolve `std.objectpool`"
                }
                val decompiledFile = PsiManager.getInstance(project).findFile(binaryFile) as? CjFile
                assertNotNull(decompiledFile, "PsiManager should restore compiled file for `std.objectpool`")

                val summary = CaStubSummaryBuilder().build(decompiledFile!!)
                val actual = renderSummary(summary)
                val expectedFile = CjoCompiledTestEnvironment.locateRepositoryRoot()
                    .resolve("analysis")
                    .resolve("stubs")
                    .resolve("testData")
                    .resolve("compiled")
                    .resolve("std.objectpool.compiled.stubs.txt")
                assertMatchesGolden(actual, expectedFile)
            }
        }
    }

    /**
     * 将单文件 stub 摘要渲染为稳定的 golden 文本。
     */
    private fun renderSummary(summary: CaStubFileSummary): String {
        return buildString {
            appendLine("fileKey=${summary.fileKey.substringAfterLast('/').substringAfterLast('\\')}")
            appendLine("kind=${summary.stubKind ?: "<missing>"}")
            appendLine("package=${summary.packageFqName?.asString() ?: "<missing>"}")
            appendLine("topLevelClassifiers=${summary.topLevelClassifierNames.map { it.asString() }.sorted()}")
            appendLine("topLevelCallables=${summary.topLevelCallableNames.map { it.asString() }.sorted()}")
            appendLine("classMembers=")
            if (summary.classMemberNames.isEmpty()) {
                append("  <none>")
            } else {
                summary.classMemberNames.toSortedMap(compareBy { it.asString() }).forEach { (classId, names) ->
                    appendLine("  ${classId.asFqNameString()}=${names.map { it.asString() }.sorted()}")
                }
            }
        }.trimEnd()
    }

    /**
     * 比较实际文本与 golden 文件；开启 `update.test.data` 时写入新的 golden 内容。
     */
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
        assertEquals(expected, normalizedActual)
    }

    /**
     * 将 Windows 换行统一为测试 golden 使用的 LF。
     */
    private fun String.normalizeLineSeparators(): String = replace("\r\n", "\n")
}
