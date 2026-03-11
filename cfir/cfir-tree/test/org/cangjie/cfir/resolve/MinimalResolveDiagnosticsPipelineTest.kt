package org.cangjie.cfir.resolve

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.moduleData
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirPackageDirective
import org.cangjie.cfir.diagnostics.CfirDiagnosticCollector
import org.cangjie.cfir.diagnostics.CfirDiagnosticReporter
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.session.diagnosticCollector
import org.cangjie.cfir.session.phaseResolverRegistry
import org.cangjie.test.config.AnalysisHandler
import org.cangjie.test.config.TestFacade
import org.cangjie.test.model.TestFile
import org.cangjie.test.model.TestModule
import org.cangjie.test.services.TestServices
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Test
import java.io.File

class MinimalResolveDiagnosticsPipelineTest {
    @Test
    fun facadeToHandlerPipelineProducesEmptyDiagnosticsBaseline() {
        val testDataFile = resolveTestDataFile()
        val module = TestModule(
            name = "main",
            files = listOf(
                TestFile(
                    name = testDataFile.name,
                    content = testDataFile.readText(Charsets.UTF_8),
                    originalFile = testDataFile,
                ),
            ),
        )

        val services = TestServices()
        val facade = MinimalResolveFacadeStep(testDataFile.name)
        val artifact = facade.transform(module, null)

        DiagnosticsHandlerStep().processModule(module, artifact, services)
    }

    private class MinimalResolveFacadeStep(
        private val fileName: String,
    ) : TestFacade {
        override fun transform(module: TestModule, input: Any?): Any {
            val session = object : CfirSession(CfirSession.Kind.Source) {}
            val diagnostics = CfirDiagnosticCollector()
            session.register(CfirModuleData::class, CfirModuleData(Name.identifier(module.name)))
            session.register(CfirPhaseResolverRegistry::class, CfirPhaseResolverRegistry())
            session.register(CfirDiagnosticReporter::class, diagnostics)
            session.register(CfirDiagnosticCollector::class, diagnostics)

            val file = CfirFile(
                moduleData = session.moduleData,
                name = fileName,
                packageDirective = CfirPackageDirective(FqName.ROOT),
            )

            CfirTotalResolveProcessor(session, session.phaseResolverRegistry).processFile(file)
            return ResolveDiagnosticsArtifact(file.name, session.diagnosticCollector.size)
        }
    }

    private class DiagnosticsHandlerStep : AnalysisHandler {
        override fun processModule(module: TestModule, artifact: Any?, testServices: TestServices) {
            val result = artifact as? ResolveDiagnosticsArtifact
                ?: error("Expected ResolveDiagnosticsArtifact, got ${artifact?.javaClass?.name}")

            val sourceFile = module.files.first().originalFile ?: error("Missing original test file")
            val expectedFile = File(sourceFile.parentFile, sourceFile.nameWithoutExtension + ".diagnostics.txt")
            val actual = buildString {
                appendLine("file: ${result.fileName}")
                appendLine("diagnostics: ${result.diagnosticsCount}")
            }.trim()

            assertEqualsToFile(expectedFile, actual)
        }
    }

    private data class ResolveDiagnosticsArtifact(
        val fileName: String,
        val diagnosticsCount: Int,
    )

    private companion object {
        const val TEST_DATA_FILE = "testData/resolveDiagnostics/smoke.cj"
        private const val UPDATE_TEST_DATA_PROPERTY = "update.test.data"

        fun resolveTestDataFile(): File {
            val moduleRelative = File(TEST_DATA_FILE)
            if (moduleRelative.exists()) return moduleRelative

            val repoRelative = File("cfir/cfir-tree/$TEST_DATA_FILE")
            if (repoRelative.exists()) return repoRelative

            error("Cannot find test data file at $TEST_DATA_FILE")
        }

        fun assertEqualsToFile(expectedFile: File, actual: String) {
            val actualTrimmed = actual.trim()
            val updateMode = java.lang.Boolean.getBoolean(UPDATE_TEST_DATA_PROPERTY)
            if (!expectedFile.exists()) {
                if (updateMode) {
                    expectedFile.parentFile.mkdirs()
                    expectedFile.writeText(actualTrimmed)
                    throw AssertionError("Golden file created: ${expectedFile.path}. Re-run test.")
                }
                throw AssertionError("Golden file missing: ${expectedFile.path}\n=== Actual ===\n$actualTrimmed")
            }

            val expected = expectedFile.readText(Charsets.UTF_8).replace("\r\n", "\n").trim()
            if (expected != actualTrimmed) {
                if (updateMode) {
                    expectedFile.writeText(actualTrimmed)
                }
                throw AssertionError("Golden file mismatch: ${expectedFile.path}\n=== Expected ===\n$expected\n=== Actual ===\n$actualTrimmed")
            }
        }
    }
}
