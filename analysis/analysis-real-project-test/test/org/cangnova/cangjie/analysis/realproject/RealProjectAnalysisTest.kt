package org.cangnova.cangjie.analysis.realproject

import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * 真实项目对照测试。
 *
 * 流程由 [RealProjectTestBase] 完成：git 检出固定提交 → `cjpm clean/build`（ground truth）→
 * cjpm 源码根 + `.cjo` 依赖组装为 standalone 模块图 → 逐文件 CFIR 诊断 → JSON 报告。
 *
 * 项目列表来自 `analysis/analysis-real-project-test/projects.properties`，
 * 或 `-PrealProjectConfig` / `-PrealProjectRepo` 指定。
 */
@Tag("realProject")
class RealProjectAnalysisTest : RealProjectTestBase(
    "analysis/analysis-real-project-test/testData/realProject",
) {
    @Test
    fun realProject(mainFile: CjFile, mainModule: CjTestModule) {
        val languageVersionSettings = (mainModule.caModule as? CaSourceModule)?.languageVersionSettings
            ?: error("测试主模块不是 CaSourceModule: ${mainModule.name}")

        runRealProjects(mainFile.project, languageVersionSettings) { session ->
            val analyses = session.analyzeAll()
            session.writeReport(analyses)
            assertNoAnalysisExceptions(session, analyses)

            if (session.config.id == "mariadb") {
                val socketAssignmentFile = session.checkout.resolve("src/connection/maria_connection_factory.cj")
                val byteBufferFile = session.checkout.resolve("src/protocol/types_string.cj")
                val socketAssignmentSource = Files.readString(socketAssignmentFile)
                val byteBufferSource = Files.readString(byteBufferFile)
                assertTrue(
                    "connection.socket = tls" in socketAssignmentSource,
                    "mariadb fixture no longer contains the reported TLS socket assignment: " + socketAssignmentFile,
                )
                assertTrue(
                    "write(0x00)" in byteBufferSource,
                    "mariadb fixture no longer contains the reported Array<Byte> write call: " + byteBufferFile,
                )

                val relevantPaths = setOf(socketAssignmentFile, byteBufferFile)
                    .mapTo(linkedSetOf()) { path -> path.toString().replace('\\', '/') }
                val regressionAnalyses = analyses.filter { fileAnalysis -> fileAnalysis.path in relevantPaths }
                assertEquals(2, regressionAnalyses.size, "必须实际分析包含两处报障代码的源码文件")
                val falseDiagnostics = regressionAnalyses.flatMap { fileAnalysis ->
                    fileAnalysis.diagnostics
                        .filter { diagnostic -> diagnostic.severity == "ERROR" }
                        .map { diagnostic ->
                            fileAnalysis.path + ": " + diagnostic.name + " " + diagnostic.snippet
                        }
                }
                assertTrue(
                    falseDiagnostics.isEmpty(),
                    "SDK " + session.config.cangjieHome +
                        " 下的 TLS 和 ByteBuffer 源文件不应有错误诊断：" +
                        falseDiagnostics.joinToString("\n"),
                )
            }
        }
    }
}
