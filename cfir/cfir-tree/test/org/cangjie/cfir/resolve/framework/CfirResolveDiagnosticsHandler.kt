package org.cangjie.cfir.resolve.framework

import org.cangjie.test.config.AnalysisHandler
import org.cangjie.test.model.TestModule
import org.cangjie.test.services.TestServices
import java.io.File

class CfirResolveDiagnosticsHandler : AnalysisHandler {
    override fun processModule(module: TestModule, artifact: Any?, testServices: TestServices) {
        val result = artifact as? ResolveDiagnosticsArtifact
            ?: error("Expected ResolveDiagnosticsArtifact, got ${artifact?.javaClass?.name}")
        val sourceFile = module.files.firstOrNull()?.originalFile ?: error("Missing original test file")
        val expectedFile = File(sourceFile.parentFile, "${sourceFile.nameWithoutExtension}.diagnostics.txt")
        val actual = buildString {
            appendLine("file: ${result.fileName}")
            appendLine("phase: ${result.declarationPhase}")
            appendLine("diagnostics: ${result.diagnostics.size}")
            result.diagnostics.forEach { diagnostic ->
                appendLine("- ${diagnostic.severity}: [${diagnostic.factoryName}] ${diagnostic.message}")
            }
        }.trim()

        assertEqualsToFile(expectedFile, actual)
    }
}

private const val UPDATE_TEST_DATA_PROPERTY = "update.test.data"

private fun assertEqualsToFile(expectedFile: File, actual: String) {
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

