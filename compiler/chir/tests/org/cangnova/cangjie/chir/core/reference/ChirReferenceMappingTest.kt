package org.cangnova.cangjie.chir.core.reference

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets

class ChirReferenceMappingTest {

    @Test
    fun `reference manifest points to existing upstream chir samples`() {
        val lines = loadManifestLines()
        val repoRoot = detectRepoRoot()

        lines.forEach { raw ->
            val parts = raw.split('|')
            require(parts.size == 4) { "invalid manifest line: $raw" }
            val id = parts[0]
            val relativePath = parts[2]
            val anchor = parts[3]

            val file = File(repoRoot, relativePath)
            assertTrue(file.exists(), "[$id] missing referenced file: $relativePath")
            val content = file.readText(StandardCharsets.UTF_8)
            assertTrue(content.contains(anchor), "[$id] missing anchor '$anchor' in $relativePath")
        }
    }

    private fun loadManifestLines(): List<String> {
        val stream = javaClass.classLoader.getResourceAsStream("chir-reference/manifest.txt")
            ?: error("missing test resource: chir-reference/manifest.txt")
        return stream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toList()
        }
    }

    private fun detectRepoRoot(): File {
        var current = File(System.getProperty("user.dir"))
        while (true) {
            val marker = File(current, "external/cangjie_compiler")
            if (marker.exists()) return current
            val parent = current.parentFile ?: error("cannot locate repository root from ${current.absolutePath}")
            current = parent
        }
    }
}
