package org.cangnova.cangjie.analysis.stubs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

class CaStubSourceGoldenTest {
    @Test
    fun sourceSummaries() {
        withEnvironment("CaStubSourceGoldenTest") { environment ->
            val fixtureDir = locateRepositoryRoot()
                .resolve("analysis")
                .resolve("stubs")
                .resolve("testData")
                .resolve("source")

            val summaryBuilder = CaStubSummaryBuilder()
            val sourceFiles = fixtureDir.listDirectoryEntries("*.cj").sortedBy { it.name }
            require(sourceFiles.isNotEmpty()) { "No source stub fixtures found under $fixtureDir" }

            sourceFiles.forEach { sourceFile ->
                val psiFile = createSourceFile(
                    environment = environment,
                    fileName = sourceFile.name,
                    text = sourceFile.readText(),
                )
                val summary = summaryBuilder.build(psiFile)
                val actual = renderSummary(summary)
                val expectedFile = sourceFile.resolveSibling("${sourceFile.fileName.toString().removeSuffix(".${sourceFile.extension}")}.stubs.txt")
                assertMatchesGolden(actual, expectedFile)
            }
        }
    }

    private fun withEnvironment(
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

    private fun createSourceFile(
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

    private fun locateRepositoryRoot(): Path {
        val start = Paths.get("").toAbsolutePath().normalize()
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
    }

    private fun String.normalizeLineSeparators(): String = replace("\r\n", "\n")
}
