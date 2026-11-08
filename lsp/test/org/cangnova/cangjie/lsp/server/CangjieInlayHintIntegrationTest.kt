package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintKind
import org.eclipse.lsp4j.InlayHintParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * 校验 inlay hint 在真实 Analysis API 工作区上的端到端行为。
 *
 * 类型提示的正确性无法在纯解析测试里验证（类型来自 `CaSession`），因此渲染结果由这里
 * 通过真实的 `document -> snapshot -> session` 链产出，并与共享层的 golden 契约文件
 * 逐字符比对——IDE 宿主（P2）接上后必须复用同一份 golden。
 */
class CangjieInlayHintIntegrationTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话，测试自行创建带夹具工作区的会话。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 整篇文档的提示必须与 golden 契约逐字符一致。
     *
     * golden 用源内标记 `<#...#>` 固化期望输出：去掉标记得到输入源码，把提示按 offset
     * 插回去必须还原出同一份文本。这样输入与期望是同一份文件，改一处不会两边漂移。
     */
    @Test
    fun `type hints match shared golden contract`() {
        val golden = readGolden("typeHints.cj")
        val source = stripMarkers(golden)
        hintsWorkspace(source).use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/hints.cj")
                testSession.openDocument(uri, source)

                val hints = testSession.inlayHint(fullDocumentParams(uri, source))

                assertTrue(hints.isNotEmpty(), "golden 文件里必须至少产出一条类型提示")
                assertTrue(
                    hints.all { it.kind == InlayHintKind.Type },
                    "该 golden 只含类型提示，实际类别：${hints.map { it.kind }}",
                )
                assertEquals(golden.trimEnd(), renderWithMarkers(source, hints, uri).trimEnd())
            }
        }
    }

    /**
     * 参数名提示必须与 golden 契约逐字符一致。
     */
    @Test
    fun `parameter name hints match shared golden contract`() {
        val golden = readGolden("parameterNames.cj")
        val source = stripMarkers(golden)
        hintsWorkspace(source).use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/hints.cj")
                testSession.openDocument(uri, source)

                val hints = testSession.inlayHint(fullDocumentParams(uri, source))

                assertTrue(hints.isNotEmpty(), "golden 文件里必须至少产出一条参数名提示")
                assertTrue(
                    hints.all { it.kind == InlayHintKind.Parameter },
                    "该 golden 只含参数名提示，实际类别：${hints.map { it.kind }}",
                )
                assertEquals(golden.trimEnd(), renderWithMarkers(source, hints, uri).trimEnd())
            }
        }
    }

    /**
     * 命名实参不产出参数名提示：形参名已经写在源码里。
     */
    @Test
    fun `named argument produces no parameter hint`() {
        val source = """
            package sample.hints

            func greet(value: Int64, times: Int64): Int64 {
                return value * times
            }

            func use(): Int64 {
                return greet(value: 1, times: 2)
            }
        """.trimIndent()

        hintsWorkspace(source).use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/hints.cj")
                testSession.openDocument(uri, source)

                assertTrue(
                    testSession.inlayHint(fullDocumentParams(uri, source)).isEmpty(),
                    "全部命名实参的调用不该有参数名提示",
                )
            }
        }
    }

    /**
     * 显式标注类型的声明不产出提示。
     */
    @Test
    fun `explicit type annotation suppresses hint`() {
        val source = """
            package sample.hints

            func demo(a: Int64): Int64 {
                let typed: Int64 = a
                return typed
            }
        """.trimIndent()

        hintsWorkspace(source).use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/hints.cj")
                testSession.openDocument(uri, source)

                val hints = testSession.inlayHint(fullDocumentParams(uri, source))

                assertEquals(emptyList<InlayHint>(), hints, "显式类型的声明不该有类型提示")
            }
        }
    }

    /**
     * 请求范围之外的位置不产出提示。
     */
    @Test
    fun `hints outside requested range are dropped`() {
        val source = """
            package sample.hints

            func demo(a: Int64): Int64 {
                let first = a
                return first
            }
        """.trimIndent()
        val signatureLine = source.lines().indexOfFirst { it.contains("func demo") }
        // 只请求函数签名那一行（非零宽，避免走「空 range 直接返回」的短路）：
        // 该行没有候选，而 `let first` 在 range 之外。
        val range = Range(Position(signatureLine, 0), Position(signatureLine, source.lines()[signatureLine].length))

        hintsWorkspace(source).use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/hints.cj")
                testSession.openDocument(uri, source)

                val scoped = testSession.inlayHint(InlayHintParams(TextDocumentIdentifier(uri), range))
                val full = testSession.inlayHint(fullDocumentParams(uri, source))

                assertTrue(scoped.isEmpty(), "range 外的提示必须被裁掉，实际：${scoped.map { it.label }}")
                assertEquals(1, full.size, "同一份文档请求全文时必须产出 range 外的那条提示")
            }
        }
    }

    /**
     * 没有任何声明的文件返回空列表而不是异常。
     */
    @Test
    fun `file without declarations yields no hints`() {
        val source = """
            package sample.hints

            // 只有注释，没有任何可提示的声明
        """.trimIndent()

        hintsWorkspace(source).use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/hints.cj")
                testSession.openDocument(uri, source)

                assertTrue(testSession.inlayHint(fullDocumentParams(uri, source)).isEmpty())
            }
        }
    }

    /**
     * P0 的 resolve 是兜底实现：必须原样返回，不能抛错让客户端丢掉提示。
     */
    @Test
    fun `inlay hint resolve returns hint unchanged`() {
        val source = """
            package sample.hints

            func demo(a: Int64): Int64 {
                let local = a
                return local
            }
        """.trimIndent()

        hintsWorkspace(source).use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/hints.cj")
                testSession.openDocument(uri, source)

                val hint = testSession.inlayHint(fullDocumentParams(uri, source)).single()
                val resolved = testSession.resolveInlayHint(hint)

                assertEquals(hint.label, resolved.label)
                assertEquals(hint.position, resolved.position)
                assertEquals(hint.kind, resolved.kind)
            }
        }
    }

    /**
     * 构造承载待测源码的单文件工作区。
     */
    private fun hintsWorkspace(source: String): LspWorkspaceFixture {
        return LspWorkspaceFixtureBuilder()
            .source("hints.cj", source)
            .build()
    }

    /**
     * 覆盖整篇文档的 hint 请求参数。
     */
    private fun fullDocumentParams(
        uri: String,
        source: String,
    ): InlayHintParams {
        val document = LspTextDocument(uri = uri, languageId = "cangjie", version = 1, text = source)
        return InlayHintParams(
            TextDocumentIdentifier(uri),
            Range(Position(0, 0), document.positionAt(source.length)),
        )
    }

    /**
     * 把提示按 offset 插回源码，还原成带标记的 golden 文本。
     */
    private fun renderWithMarkers(
        source: String,
        hints: List<InlayHint>,
        uri: String,
    ): String {
        val document = LspTextDocument(uri = uri, languageId = "cangjie", version = 1, text = source)
        val marked = hints
            .map { hint -> document.offsetAt(hint.position) to hint.label.left }
            .sortedByDescending { it.first }
        return marked.fold(source) { text, (offset, label) ->
            text.substring(0, offset) + MARKER_OPEN + label + MARKER_CLOSE + text.substring(offset)
        }
    }

    /**
     * 去掉 golden 里的源内标记，得到交给服务端的源码。
     */
    private fun stripMarkers(golden: String): String = golden.replace(MARKER_PATTERN, "")

    /**
     * 读取共享 golden 契约文件。
     *
     * 从工作目录向上找 `settings.gradle.kts` 定位仓库根，避免依赖 Gradle 的测试工作目录约定。
     */
    private fun readGolden(relativePath: String): String {
        var current: Path? = Path.of("").toAbsolutePath()
        while (current != null) {
            val candidate = current.resolve("code-insight").resolve("hints").resolve("testData").resolve(relativePath)
            if (Files.exists(candidate)) return Files.readString(candidate)
            current = current.parent
        }
        error("找不到 golden 契约文件 code-insight/hints/testData/$relativePath")
    }

    private companion object {
        /** golden 标记起始符。 */
        private const val MARKER_OPEN = "<#"

        /** golden 标记结束符。 */
        private const val MARKER_CLOSE = "#>"

        /** 标记匹配模式，用于把 golden 还原成源码。 */
        private val MARKER_PATTERN = Regex("<#.*?#>", RegexOption.DOT_MATCHES_ALL)
    }
}
