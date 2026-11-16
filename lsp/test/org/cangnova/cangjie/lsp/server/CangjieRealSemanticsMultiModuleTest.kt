package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.ReferenceContext
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 多模块工作区：`app` 依赖 `lib`，两者各有独立的源码根。
 *
 * 单模块测试里所有文件同属一个 source set，符号查找天然可见；真实工程是多模块的，
 * 「跨模块可见」需要项目结构把两个模块都装进去。这里断言的是**跨模块语义**：
 * 依赖能解析（无诊断）、能跳到另一个模块的声明、反向引用能找回来。
 */
class CangjieRealSemanticsMultiModuleTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：需要多模块初始化参数。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 依赖模块的公开函数必须可解析：`app` 里调用 `helper()` 不得产生诊断。
     */
    @Test
    fun `dependency module symbols resolve without diagnostics`() {
        multiModuleWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val appUri = fixture.uri(APP)
                session.openDocument(appUri, fixture.text(APP))

                val diagnostics = session.awaitPublishedDiagnostics(appUri).diagnostics
                val errors = diagnostics.filter { it.severity == DiagnosticSeverity.Error }
                assertTrue(
                    errors.isEmpty(),
                    "跨模块依赖必须解析成功，实际诊断：${errors.map(::render)}",
                )
            }
        }
    }

    /**
     * 从 `app` 跳到 `lib` 的声明必须落到另一个模块的文件。
     */
    @Test
    fun `definition crosses the module boundary`() {
        multiModuleWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val appUri = fixture.uri(APP)
                val libUri = fixture.uri(LIB)
                session.openDocument(appUri, fixture.text(APP))
                session.openDocument(libUri, fixture.text(LIB))

                val call = positionOf(fixture, APP, "helper()")
                val definition = session.definition(DefinitionParams(TextDocumentIdentifier(appUri), call))
                    .let { if (it.isLeft) it.left.orEmpty() else emptyList<Location>() }

                assertTrue(
                    definition.any { it.uri == libUri },
                    "app 的 helper() 必须能跳到 lib 模块的声明，实际 $definition",
                )
                assertTrue(
                    definition.any { it.uri == libUri && covers(it, lineOf(fixture, LIB, "public func helper")) },
                    "跳转目标必须是 lib 的 public func helper，实际 $definition",
                )
            }
        }
    }

    /**
     * 反向引用必须跨模块：在 `lib` 的声明上查引用，要能找到 `app` 的调用点。
     *
     * 只测正向跳转时，「模块内符号表各自独立、只有 import 解析走桥接」的实现也能过；
     * 反向查找会真的遍历依赖方模块。
     */
    @Test
    fun `references cross the module boundary`() {
        multiModuleWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val appUri = fixture.uri(APP)
                val libUri = fixture.uri(LIB)
                session.openDocument(appUri, fixture.text(APP))
                session.openDocument(libUri, fixture.text(LIB))

                val declaration = positionOf(fixture, LIB, "func helper", delta = "func ".length)
                val references = session.references(
                    ReferenceParams(TextDocumentIdentifier(libUri), declaration, ReferenceContext(true)),
                )

                assertTrue(
                    references.any { it.uri == appUri },
                    "lib 的 helper 声明必须能反查到 app 模块的调用点，实际 $references",
                )
                assertTrue(
                    references.any { it.uri == appUri && covers(it, lineOf(fixture, APP, "helper()")) },
                    "反向引用必须落在 app 的 helper() 调用行，实际 $references",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    private companion object {
        const val APP: String = "app/src/main.cj"
        const val LIB: String = "lib/src/helper.cj"
    }

    /**
     * 两个源码模块：`app` 依赖 `lib`。
     *
     * 两个模块各有独立源码根，`app` 通过 `requires` 声明对 `lib` 的依赖；
     * 文件内容来自 `testData/multiModule/`。
     */
    private fun multiModuleWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .addModule(
            name = "lib",
            rootRelativePath = "lib",
            sourceRoots = listOf("src"),
            requiredModuleNames = emptyList(),
        )
        .addModule(
            name = "app",
            rootRelativePath = "app",
            sourceRoots = listOf("src"),
            requiredModuleNames = listOf("lib"),
        )
        .fileFromTestData(LIB, "multiModule/lib/helper.cj")
        .fileFromTestData(APP, "multiModule/app/main.cj")
        .build()

    // ---------------------------------------------------------------- 断言辅助

    /** 位置是否落在指定行上。 */
    private fun covers(location: Location, line: Int): Boolean =
        location.range.start.line <= line && location.range.end.line >= line

    /** token 起始位置（可用 [delta] 在 token 内部偏移）。 */
    private fun positionOf(
        fixture: LspWorkspaceFixture,
        relativePath: String,
        token: String,
        delta: Int = 0,
    ): Position {
        val text = fixture.text(relativePath)
        val offset = text.indexOf(token)
        check(offset >= 0) { "在 $relativePath 中找不到 `$token`" }
        return LspTextDocument(fixture.uri(relativePath), "cangjie", 1, text).positionAt(offset + delta)
    }

    /** token 所在行。 */
    private fun lineOf(fixture: LspWorkspaceFixture, relativePath: String, token: String): Int =
        positionOf(fixture, relativePath, token).line

    /** 诊断的可读描述。 */
    private fun render(diagnostic: Diagnostic): String =
        "${diagnostic.range.start.line}:${diagnostic.range.start.character} ${diagnostic.message.left}"
}
