package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 LSP codeAction 两段链路的集成行为。
 *
 * 覆盖：
 * 1. 列表阶段按 quick-fix 框架给出「实现成员」动作，且不携带编辑；
 * 2. resolve 阶段经 headless 成员生成产出真实插入编辑；
 * 3. 无可修复诊断的文档返回空列表。
 */
class CangjieCodeActionIntegrationTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话，测试使用自定义工作区夹具。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 校验「列表不携带编辑、resolve 产出 override 成员插入」两段契约。
     */
    @Test
    fun `codeAction lists quickfix and resolve produces implement-member edits`() {
        quickFixWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/host.cj")
                testSession.openDocument(uri, fixture.text("src/host.cj"))

                val classRange = rangeOf(fixture, "src/host.cj", "class Host")
                val actions = testSession.codeAction(
                    CodeActionParams(
                        TextDocumentIdentifier(uri),
                        classRange,
                        CodeActionContext(emptyList()),
                    ),
                )

                val quickFixes = actions.mapNotNull { if (it.isRight) it.right else null }
                    .filter { it.kind == CodeActionKind.QuickFix }
                assertTrue(
                    quickFixes.isNotEmpty(),
                    "未实现成员的诊断必须给出 quick fix，实际 $actions",
                )
                quickFixes.forEach { action ->
                    assertNull(action.edit, "codeAction 列表阶段不得携带编辑，编辑应延迟到 resolve。")
                    assertNotNull(action.data, "resolve 需要 data 携带诊断定位信息。")
                }

                val resolved = testSession.resolveCodeAction(quickFixes.first())
                val edits = resolved.edit?.changes?.get(uri).orEmpty()
                assertTrue(edits.isNotEmpty(), "resolve 必须产出真实编辑。")
                assertTrue(
                    edits.any { it.newText.contains("override func greet") },
                    "插入编辑必须包含 override 成员声明，实际 ${edits.map { it.newText }}",
                )
            }
        }
    }

    /**
     * 校验没有可修复诊断的文档返回空 codeAction 列表。
     */
    @Test
    fun `codeAction returns empty for file without fixable diagnostics`() {
        LspWorkspaceFixtureBuilder()
            .source(
                "clean.cj",
                """
                    package sample.fix

                    class Clean {
                        func greet(): Unit {
                            return
                        }
                    }
                """.trimIndent(),
            )
            .build()
            .use { fixture ->
                createSession(fixture.initializeParams()).use { testSession ->
                    val uri = fixture.uri("src/clean.cj")
                    testSession.openDocument(uri, fixture.text("src/clean.cj"))

                    val classRange = rangeOf(fixture, "src/clean.cj", "class Clean")
                    val actions = testSession.codeAction(
                        CodeActionParams(
                            TextDocumentIdentifier(uri),
                            classRange,
                            CodeActionContext(emptyList()),
                        ),
                    )

                    assertFalse(actions.isNotEmpty(), "无可修复诊断时 codeAction 必须为空列表。")
                }
            }
    }

    /**
     * 构造「接口成员未实现」工作区。
     */
    private fun quickFixWorkspace(): LspWorkspaceFixture {
        return LspWorkspaceFixtureBuilder()
            .source(
                "host.cj",
                """
                    package sample.fix

                    interface Greeter {
                        func greet(name: String): String
                    }

                    class Host <: Greeter {
                    }
                """.trimIndent(),
            )
            .build()
    }

    /**
     * 计算夹具文件中指定 token 覆盖的 LSP 范围。
     */
    private fun rangeOf(fixture: LspWorkspaceFixture, relativePath: String, token: String): Range {
        val text = fixture.text(relativePath)
        val offset = text.indexOf(token)
        check(offset >= 0) { "Cannot find `$token` in $relativePath" }
        val document = LspTextDocument(
            uri = fixture.uri(relativePath),
            languageId = "cangjie",
            version = 1,
            text = text,
        )
        return Range(
            document.positionAt(offset),
            document.positionAt(offset + token.length),
        )
    }
}
