package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportTargetData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.ImportConflict
import org.cangnova.cangjie.ide.completion.api.lookup.ImportConflictKind
import org.cangnova.cangjie.ide.completion.api.lookup.ImportResolution
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.cangnova.cangjie.name.FqName
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextEdit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * LSP 侧 import 编辑回归。
 *
 * 断言方式是**把编辑应用到原文**再看结果文本：只断言「产生了一条编辑」无法区分
 * 「插在包声明之后」与「插在文件末尾」，而后者会产出无法解析的文件。
 *
 * 用真实 LSP 容器（不是自建夹具）的原因见设计 §12.7：导入应用器要读 PSI 的包声明与既有导入，
 * 容器装配不同会让结论不可外推。
 *
 * 断言里的 import 文本是**包路径**而不是成员路径（`import std.collection`，不是
 * `import std.collection.HashMap`）：`ImportPath.fqName` 是「被导入目标的完整限定名」，
 * 而仓颉里 `import a.b` 就能引用 `a.b` 的成员（差异表 #9，cjc 实测），
 * 各 section 因此在计划里写包路径。
 */
class LspImportEditCalculatorTest : AbstractLspIntegrationTest() {
    /**
     * 本类自建连接与工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 未导入的候选：在包声明之后插入 import。
     */
    @Test
    fun insertsImportAfterPackageDirective() {
        inWorkspace(PLAIN_SOURCE) { project, uri ->
            val edits = LspImportEditCalculator.importEdits(
                project = project,
                documentUri = uri,
                fileName = SOURCE_NAME,
                text = PLAIN_SOURCE,
                snapshot = candidateRequiringImport("HashMap"),
            )

            assertEquals(1, edits.size, "未导入时应恰好产生一条 import 编辑，实际 $edits")
            val updated = apply(PLAIN_SOURCE, edits)
            assertTrue(
                updated.contains("import std.collection"),
                "结果文本必须含新 import，实际 $updated",
            )
            assertTrue(
                updated.indexOf("import std.collection") > updated.indexOf("package sample.imports"),
                "import 必须落在包声明之后，实际 $updated",
            )
            assertTrue(
                updated.contains("func use(): Int64"),
                "既有声明不得被改写，实际 $updated",
            )
        }
    }

    /**
     * 已导入时不产生编辑：重复写一条 import 会让用户看到两份同样的导入。
     */
    @Test
    fun alreadyImportedProducesNoEdit() {
        inWorkspace(IMPORTED_SOURCE) { project, uri ->
            val edits = LspImportEditCalculator.importEdits(
                project = project,
                documentUri = uri,
                fileName = SOURCE_NAME,
                text = IMPORTED_SOURCE,
                snapshot = candidateRequiringImport("HashMap"),
            )

            assertTrue(edits.isEmpty(), "已导入时不得产生编辑（幂等），实际 $edits")
        }
    }

    /**
     * 不需要导入的候选不产生编辑。
     */
    @Test
    fun noImportPlanProducesNoEdit() {
        inWorkspace(PLAIN_SOURCE) { project, uri ->
            val edits = LspImportEditCalculator.importEdits(
                project = project,
                documentUri = uri,
                fileName = SOURCE_NAME,
                text = PLAIN_SOURCE,
                snapshot = completionSnapshot(
                    lookupString = "use",
                    kind = CompletionItemKind.LOCAL,
                    origin = CompletionOrigin.LOCAL_SCOPE,
                ).build(),
            )

            assertTrue(edits.isEmpty(), "无需导入的候选不得产生编辑，实际 $edits")
        }
    }

    /**
     * 短名被占用时**明确失败**，不插一个指向别处的短名。
     */
    @Test
    fun conflictFailsInsteadOfWritingAmbiguousShortName() {
        inWorkspace(PLAIN_SOURCE) { project, uri ->
            val failing = assertThrows(IllegalStateException::class.java) {
                LspImportEditCalculator.importEdits(
                    project = project,
                    documentUri = uri,
                    fileName = SOURCE_NAME,
                    text = PLAIN_SOURCE,
                    snapshot = candidateRequiringImport(
                        "HashMap",
                        conflict = ImportConflict(ImportConflictKind.SHORT_NAME_TAKEN_BY_OTHER, "sample.Other.HashMap"),
                    ),
                )
            }

            assertTrue(
                failing.message?.contains("短名已被占用") == true,
                "失败信息必须说明原因（短名冲突），实际 ${failing.message}",
            )
        }
    }

    /**
     * 在真实工作区里执行断言。
     */
    private fun inWorkspace(
        source: String,
        body: (com.intellij.openapi.project.Project, String) -> Unit,
    ) {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(SOURCE_NAME, source)
            .build()
        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            val uri = fixture.uri("src/$SOURCE_NAME")
            val project = connection.server.context.environment.project
            body(project, uri)
        }
    }

    /**
     * 需要追加 import 的候选。
     */
    private fun candidateRequiringImport(
        name: String,
        conflict: ImportConflict? = null,
    ): CompletionSnapshot = completionSnapshot(
        lookupString = name,
        kind = CompletionItemKind.TOP_LEVEL,
        origin = CompletionOrigin.TOP_LEVEL,
    ).apply {
        importPlan = ImportResolution(
            CompletionImportPlan.AddImport(
                CompletionImportTargetData(ImportPath(FqName.fromString("std.collection"), false), name),
            ),
            conflict = conflict,
        )
    }.build()

    /**
     * 把编辑应用到原文（本类的编辑都相对原文，且只有一条或零条）。
     */
    private fun apply(text: String, edits: List<TextEdit>): String {
        val out = StringBuilder()
        var cursor = 0
        edits.sortedBy { offsetOf(text, it.range.start) }.forEach { edit ->
            val start = offsetOf(text, edit.range.start)
            val end = offsetOf(text, edit.range.end)
            out.append(text, cursor, start).append(edit.newText)
            cursor = end
        }
        out.append(text, cursor, text.length)
        return out.toString()
    }

    /**
     * 位置 → 偏移（夹具源码只用 `\n`）。
     */
    private fun offsetOf(text: String, position: Position): Int {
        var line = 0
        var offset = 0
        while (line < position.line && offset < text.length) {
            val newline = text.indexOf('\n', offset)
            if (newline < 0) break
            offset = newline + 1
            line++
        }
        return offset + position.character
    }

    private companion object {
        const val SOURCE_NAME = "use.cj"

        /** 没有任何 import 的文件。 */
        val PLAIN_SOURCE = """
            package sample.imports

            func use(): Int64 {
                return 1
            }
        """.trimIndent()

        /** 已经导入目标包的文件（仓颉里 `import a.b` 即可引用其成员，见差异表 #9）。 */
        val IMPORTED_SOURCE = """
            package sample.imports

            import std.collection

            func use(): Int64 {
                return 1
            }
        """.trimIndent()
    }
}
