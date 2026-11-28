package org.cangnova.cangjie.lsp.completion

import com.google.gson.JsonObject
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.ide.completion.impl.shared.resolve.completionDeclarationResolverOrNull
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `completionItem/resolve`：延迟文档（设计 §7.11.3 的 `documentation` 一行）。
 *
 * 三条要验证的事：
 * 1. **有 CDoc 的候选**在 resolve 后拿到 Markdown 文档（与 hover 同一份渲染）；
 * 2. **没有 CDoc 的候选**resolve 后不带文档，且候选本身不变（失败不吞候选）；
 * 3. 会话已过期时 resolve **不报错**、原样返回——客户端可能拿很旧的 item 来 resolve。
 */
class LspCompletionResolveTest : AbstractLspIntegrationTest() {
    /**
     * 本类自建连接与工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * **恢复这半**：按候选身份重进分析必须恢复出**同一个声明**。
     *
     * 这是 resolve 的前半段，也是唯一与「本容器能不能拿到 CDoc」无关的半段。
     * 它验证身份键算法一致：快照里的 `qualifiedName` 由生成候选的那一层算出，
     * 恢复端必须用同一套算法才对得上——对不上时 resolve 只是静默不填文档，看不出哪一半坏了。
     *
     * 使用点取**磁盘上的夹具文件**（真实文件有完整 PSI），而不是 LSP 的 light 快照：
     * 这里验的是恢复算法，不是 LSP 文件形态。
     */
    @Test
    fun resolverRecoversTheSameDeclarationByCandidateIdentity() {
        withSession { connection, uri, text ->
            val project = connection.server.context.environment.project
            val item = completionItemFor(connection, uri, text, "documented")
            val data = item.data as JsonObject
            val session = connection.server.context.completionSessions
                .find(data.get(LspCompletionCommands.SESSION_KEY_FIELD).asString)
                ?: error("补全必须留下会话")
            val snapshot = session.itemAt(data.get(LspCompletionCommands.INDEX_FIELD).asInt)
                ?: error("会话里必须有该候选快照")
            val resolver = completionDeclarationResolverOrNull(project)
                ?: error("宿主必须装配候选声明解析器（LSP 注册器注册服务、提供者装入实现）")

            val useSite = useSiteElementOf(project, uri, text)

            // 恢复必须在分析会话内进行并立刻取名：符号不得跨会话带出。
            val resolvedName = org.cangnova.cangjie.analysis.api.analyze(useSite.containingCjFile!!) {
                resolver.resolveDeclaration(this@analyze, snapshot) { symbol ->
                    (symbol as? CaNamedSymbol)?.name?.asString()
                }
            }

            assertEquals(
                "documented",
                resolvedName,
                "按身份必须恢复出同一个声明，快照身份=${snapshot.identity.qualifiedName}",
            )
        }
    }

    /**
     * 造一份可分析的 PSI 并取其中一处元素作为使用点。
     *
     * 刻意**不去打开磁盘夹具文件**：本容器里 `LocalFileSystem.getInstance()` 返回的是
     * `CoreLocalFileSystem`（实测强转失败），而这里要验的是恢复算法本身，
     * 用与补全路径同源的 light PSI 即可——它同样能进分析。
     */
    private fun useSiteElementOf(
        project: com.intellij.openapi.project.Project,
        uri: String,
        text: String,
    ): CjElement {
        val file = org.cangnova.cangjie.lsp.analysis.LspAnalysisPsiFileFactory
            .createFile(project, uri, "use.cj", text)
        val leaf = file.findElementAt(text.indexOf("consume()"))
            ?: error("使用点偏移处没有元素")
        var current: PsiElement? = leaf
        while (current != null && current !is CjElement) current = current.parent
        return current as? CjElement ?: error("使用点不在仓颉 PSI 里")
    }

    /**
     * **有 CDoc 的候选**：resolve 后必须拿到 Markdown 文档（设计 §7.11.3 的 `documentation` 一行）。
     *
     * 这条曾经是反过来的：彼时它断言「本容器里填不出文档」并写明「容器哪天支持了它会失败，
     * 提醒改成正向用例」。2026-10-11 查到真因——`parseImportDirectives` 里一段被注释掉的注释绑定器
     * （`DoNotBindAnything`，注释原文「允许注释绑定到首个声明」）导致**没有 import 的文件**里
     * `package` 之后的文档注释被绑定进 import list 子树，声明的 `docComment` 因此恒空且不报错。
     * 恢复该绑定器后，身份恢复 + CDoc 渲染两半合起来，文档确实被填上——本用例即其端到端验收。
     */
    @Test
    fun resolveFillsDocumentationForDocumentedCandidate() {
        withSession { connection, uri, text ->
            val resolved = connection.resolveCompletionItem(completionItemFor(connection, uri, text, "documented"))

            val documentation = resolved.documentation?.right?.value
            assertTrue(
                !documentation.isNullOrBlank(),
                "有 CDoc 的候选 resolve 后必须带文档，实际 ${resolved.documentation}",
            )
            assertTrue(
                documentation!!.contains("被记录的声明"),
                "文档内容必须来自该声明的 CDoc，实际 ${documentation}",
            )
        }
    }
    /**
     * 没有 CDoc 的候选：resolve 后不带文档，但候选的其余字段不变。
     */
    @Test
    fun resolveLeavesItemUnchangedWhenNoCDoc() {
        withSession { connection, uri, text ->
            val item = completionItemFor(connection, uri, text, "undocumented")

            val resolved = connection.resolveCompletionItem(item)

            assertNull(resolved.documentation, "没有 CDoc 时不得凭空空填文档")
            assertEquals(item.label, resolved.label, "resolve 不得改动候选本身")
            assertEquals(item.sortText, resolved.sortText, "resolve 不得改动排序文本")
        }
    }

    /**
     * 会话已过期：resolve 原样返回，不报错。
     */
    @Test
    fun resolveToleratesExpiredSession() {
        withSession { connection, uri, text ->
            val item = completionItemFor(connection, uri, text, "documented")
            // 伪造一个不存在的会话键：等价于「客户端拿很旧的 item 来 resolve」。
            (item.data as com.google.gson.JsonObject).addProperty(LspCompletionCommands.SESSION_KEY_FIELD, "cj-completion-404")

            val resolved = connection.resolveCompletionItem(item)

            assertNull(resolved.documentation, "会话过期时不得填文档")
            assertEquals(item.label, resolved.label, "会话过期时也必须原样返回候选")
        }
    }

    /**
     * 取一个指定名字的候选。
     */
    private fun completionItemFor(
        connection: LspIntegrationTestConnection,
        uri: String,
        text: String,
        candidateName: String,
    ): CompletionItem {
        val items = connection.completion(
            CompletionParams(TextDocumentIdentifier(uri), positionAt(text)),
        ).left
        return items.firstOrNull { it.label == candidateName }
            ?: error("夹具里没有候选 `$candidateName`，实际 ${items.map(CompletionItem::getLabel)}")
    }

    /**
     * 造夹具、开会话，并把文档 PSI 绑定推到位。
     */
    private fun withSession(body: (LspIntegrationTestConnection, String, String) -> Unit) {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(USE_NAME, USE_SOURCE)
            .build()
        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            val uri = fixture.uri("src/$USE_NAME")
            val text = fixture.text("src/$USE_NAME")
            connection.openDocument(uri, text)
            // 打开文档的副作用靠一次协议请求推到完成。
            connection.documentSymbol(connection.documentSymbolParams(uri))
            body(connection, uri, text)
        }
    }

    /**
     * 偏移 → LSP 位置（夹具源码只用 `\n`）；取 `let u =` 之后（空前缀）。
     */
    private fun positionAt(text: String): Position {
        val offset = text.indexOf("let u =") + "let u =".length
        val prefix = text.substring(0, offset)
        return Position(prefix.count { it == '\n' }, offset - (prefix.lastIndexOf('\n') + 1))
    }

    private companion object {
        const val USE_NAME = "use.cj"

        /**
         * 一个带 CDoc 的顶层函数、一个不带的，以及触发补全的位置。
         *
         * CDoc 用仓颉的块注释形式（`/** */`）——与 `docProvider` 夹具一致；
         * 三斜线形式在这里不被文档提供者识别（写错形式的表现正是「resolve 后文档为空」）。
         */
        val USE_SOURCE = """
            package sample.resolve

            /**
             * 被记录的声明。
             *
             * 第二段说明。
             */
            func documented(): Int64 {
                1
            }

            func undocumented(): Int64 {
                2
            }

            func consume(): Int64 {
                let u =
                return 1
            }
        """.trimIndent()
    }
}
