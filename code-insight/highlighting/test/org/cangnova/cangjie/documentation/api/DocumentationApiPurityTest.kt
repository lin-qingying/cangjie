package org.cangnova.cangjie.documentation.api

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class DocumentationApiPurityTest {
    @Test
    fun `API source has no platform PSI or analysis dependencies`() {
        val apiDirectory = repositoryRoot()
            .resolve("code-insight/highlighting/src/org/cangnova/cangjie/documentation/api")
        assertTrue(Files.isDirectory(apiDirectory), "Missing API source directory: $apiDirectory")

        val forbiddenImports = Regex(
            "^\\s*import\\s+(?:com\\.intellij\\.|org\\.cangnova\\.cangjie\\.psi\\.|org\\.cangnova\\.cangjie\\.analysis\\.)",
        )
        val forbiddenTypes = Regex("\\b(?:PsiElement|Project|TextAttributes)\\b")
        val violations = mutableListOf<String>()

        Files.walk(apiDirectory).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                .forEach { file ->
                    Files.readAllLines(file).forEachIndexed { index, line ->
                        if (forbiddenImports.containsMatchIn(line) || forbiddenTypes.containsMatchIn(line)) {
                            violations += "${repositoryRoot().relativize(file)}:${index + 1}: $line"
                        }
                    }
                }
        }

        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    @Test
    fun `renderer contract accepts a lambda and all section families remain representable`() {
        val entries = listOf(NamedEntry("item", "description", null))
        val sections = listOf(
            ParametersSection(entries),
            TypeParametersSection(entries),
            PropertiesSection(entries),
            ThrowsSection(entries),
            ConstructorSection("constructor"),
            ReturnsSection("returns"),
            SinceSection("since"),
            SuppressSection("suppress"),
            AuthorsSection(listOf("Ada")),
            SeeAlsoSection(listOf(LinkEntry("Target", "details", null))),
            SamplesSection(listOf(SampleEntry("sample", null))),
        )
        val content = DocumentationContent("func f()", DocumentationBody(sections), null)
        val renderer: DocumentationRenderer<String> = DocumentationRenderer { it.definition }

        assertTrue(sections.size == 11)
        assertTrue(renderer.render(content) == "func f()")
    }

    private fun repositoryRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (current != current.root) {
            if (Files.isRegularFile(current.resolve("settings.gradle.kts"))) return current
            current = current.parent
        }
        error("Cannot locate repository root from ${Path.of("").toAbsolutePath()}")
    }
}
