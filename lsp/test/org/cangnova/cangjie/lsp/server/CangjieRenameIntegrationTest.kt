package org.cangnova.cangjie.lsp.server

import com.intellij.refactoring.rename.RenameInputValidator
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PrepareRenameParams
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 LSP rename 请求与 headless 重构能力的集成行为。
 *
 * 该测试覆盖跨文件 workspace edit、prepareRename 范围和无头环境扩展注册。
 */
class CangjieRenameIntegrationTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话，测试使用自定义多文件工作区夹具。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 校验 rename 会同时修改声明和引用所在文件。
     */
    @Test
    fun `rename returns declaration and reference workspace edits`() {
        renameWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val modelUri = fixture.uri("src/model.cj")
                val useUri = fixture.uri("src/use.cj")
                testSession.openDocument(modelUri, fixture.text("src/model.cj"))
                testSession.openDocument(useUri, fixture.text("src/use.cj"))

                val prepareRename = testSession.prepareRename(
                    PrepareRenameParams(
                        TextDocumentIdentifier(useUri),
                        positionOf(fixture, "src/use.cj", "greeter.message", delta = 11),
                    ),
                )
                val edit = testSession.rename(
                    RenameParams(
                        TextDocumentIdentifier(useUri),
                        positionOf(fixture, "src/use.cj", "greeter.message", delta = 11),
                        "renamedMessage",
                    ),
                )

                val prepareResult = prepareRename.second
                assertNotNull(prepareResult, "prepareRename 必须返回真实可编辑范围。")
                assertEquals("message", prepareResult.placeholder)

                val changes = edit.changes ?: emptyMap()
                assertFalse(changes.isEmpty(), "rename 必须返回真实 WorkspaceEdit。")
                assertEquals(2, changes.size)

                val modelEdits = changes[modelUri].orEmpty()
                val useEdits = changes[useUri].orEmpty()
                assertEquals(1, modelEdits.size)
                assertEquals("renamedMessage", modelEdits.single().newText)
                assertEquals(2, useEdits.size)
                useEdits.forEach { textEdit ->
                    assertEquals("renamedMessage", textEdit.newText)
                }
            }
        }
    }

    /**
     * 校验 headless LSP 容器加载仓颉 rename 输入校验器。
     */
    @Test
    fun `headless environment loads rename input validator`() {
        renameWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use {
                val registered = RenameInputValidator.EP_NAME.extensionList.any { validator ->
                    validator::class.java.name ==
                        "org.cangnova.cangjie.codeinsight.refactoring.rename.CangJieDeclarationRenameInputValidator"
                }

                assertTrue(registered, "LSP headless 容器必须加载仓颉 rename 输入校验器。")
            }
        }
    }

    /**
     * 构造重命名测试使用的声明文件和引用文件工作区。
     */
    private fun renameWorkspace(): LspWorkspaceFixture {
        return LspWorkspaceFixtureBuilder()
            .source(
                "model.cj",
                """
                    package sample.rename

                    class Greeter {
                        let message = "hello"
                    }
                """.trimIndent(),
            )
            .source(
                "use.cj",
                """
                    package sample.rename

                    func useGreeting(greeter: Greeter): String {
                        let current = greeter.message
                        return greeter.message
                    }
                """.trimIndent(),
            )
            .build()
    }

    /**
     * 计算夹具文件中指定 token 的 LSP 位置。
     */
    private fun positionOf(
        fixture: LspWorkspaceFixture,
        relativePath: String,
        token: String,
        occurrence: Int = 1,
        delta: Int = 0,
    ): Position {
        val text = fixture.text(relativePath)
        var searchFrom = 0
        var offset = -1
        repeat(occurrence) {
            offset = text.indexOf(token, searchFrom)
            check(offset >= 0) { "Cannot find `$token` in $relativePath" }
            searchFrom = offset + token.length
        }
        val document = LspTextDocument(
            uri = fixture.uri(relativePath),
            languageId = "cangjie",
            version = 1,
            text = text,
        )
        return document.positionAt(offset + delta)
    }
}
