package org.cangnova.cangjie.chir.core.reference

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 校验 CHIR 官方参考映射清单与上游源码样本的一致性。
 *
 * 该测试读取测试资源中的 manifest，并在存在 `external/cangjie_compiler` 时确认每个引用文件和锚点真实存在。
 */
class ChirReferenceMappingTest {

    /**
     * 校验参考清单中的上游 CHIR 样本路径和锚点可解析。
     *
     * 当前工作区缺少或未填充官方编译器镜像时，该用例通过 JUnit assumption 跳过而不制造误报。
     */
    @Test
    fun `reference manifest points to existing upstream chir samples`() {
        val lines = loadManifestLines()
        val repoRoot = detectRepoRoot() ?: run {
            assumeTrue(false, "skip chir reference mapping: external/cangjie_compiler is unavailable")
            return
        }
        val externalRoot = File(repoRoot, "external/cangjie_compiler")
        assumeTrue(
            externalRoot.walkTopDown().any { it.isFile },
            "skip chir reference mapping: external/cangjie_compiler is empty in current workspace",
        )

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

    /**
     * 从测试资源中加载有效的参考清单行。
     *
     * 该方法忽略空行和注释行，返回后续路径校验所需的原始字段记录。
     */
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

    /**
     * 从当前测试工作目录向上查找仓库根目录。
     *
     * 查找以 `external/cangjie_compiler` 目录作为仓库根标记，允许测试在不同 Gradle 工作目录下运行。
     */
    private fun detectRepoRoot(): File? {
        var current = File(System.getProperty("user.dir"))
        while (true) {
            val marker = File(current, "external/cangjie_compiler")
            if (marker.exists()) {
                return current
            }
            val parent = current.parentFile ?: return null
            current = parent
        }
    }
}
