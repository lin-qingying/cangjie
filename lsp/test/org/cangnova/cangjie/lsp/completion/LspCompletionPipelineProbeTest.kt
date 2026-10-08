package org.cangnova.cangjie.lsp.completion

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.ide.completion.api.CompletionKind
import org.cangnova.cangjie.ide.completion.impl.cfir.cfirCompletionServiceOrNull
import org.cangnova.cangjie.ide.completion.impl.shared.PlatformCompletionRequest
import org.cangnova.cangjie.ide.completion.impl.shared.dummy.CompletionDummyIdentifierProvider
import org.cangnova.cangjie.lsp.analysis.LspAnalysisPsiFileFactory
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 15.1 ② 的实验：**dummy 修正必须在副本上做**，副本能否被分析——已实测**可行**，本类锁定它。
 *
 * 背景：新补全路径（分支 B）必须先做 dummy 修正，而 `provideFor` 是一次 PSI 写。
 * 在 LSP 宿主里直接改写文档所拥有的那份 `CjFile` 会污染 `LspDocumentStore`
 * （之后所有诊断/hover 都会看到占位符），因此形态是「自建副本 → 在副本上修正 → 分析副本」。
 * 与既有旧路径的区别：旧路径只在**完整标识符**上取引用候选、不改写 PSI，
 * 所以它的「直接分析 LSP 文件」不能证明新路径可行——这正是 15.1 ② 需要补实验的原因。
 *
 * 本类同时锁住三条**在生产容器里实测出来的**约束，缺一条的表现都是静默零候选：
 * 1. `CangJieCompletionServiceRegistry` 与 `CfirCompletionServiceProvider` 都必须注册
 *    （后者内部走 `project.service()` 取前者，异常被 `runCatching` 吞掉 → `null`）；
 * 2. dummy 修正是 PSI 写，必须在 **EDT + 写命令**内执行——本线程直接调
 *    `CommandProcessor.executeCommand` 会**静默不执行**（实测捕获变量仍为 null）；
 * 3. `CangJieCompletionService.complete` 需要**进度指示器**，RPC 线程与测试线程都没有。
 */
class LspCompletionPipelineProbeTest : AbstractLspIntegrationTest() {
    /**
     * 该实验自建连接与工作区，不使用基类的默认会话。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 造副本 → 命令内修正 → 送进共享管线，断言能拿到候选。
     */
    @Test
    fun dummyCorrectedCopyYieldsCandidates() {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(MODEL_NAME, MODEL_SOURCE)
            .source(USE_NAME, USE_SOURCE)
            .build()

        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()

            val uri = fixture.uri(USE_FILE)
            val server = connection.server
            val project = server.context.environment.project
            val context = server.context.requestContext()
            val text = fixture.text(USE_FILE)
            val caret = text.indexOf(PARTIAL_NAME) + PARTIAL_NAME.length

            /*
             * 直接调用门面（不经 RPC）：异常能拿到真实栈，而不是被 LSP4J 折成 `Internal error.`；
             * 这一步同时把文档 PSI 与 use-site 模块绑定起来（`upsertSnapshot` 由语义请求触发），
             * 并顺带给出旧路径在本夹具上的对照结果。
             */
            val document = LspTextDocument(uri = uri, languageId = "cangjie", version = 1, text = text)
            server.context.analysisFacade.completion(
                context,
                document,
                CompletionParams(TextDocumentIdentifier(uri), positionAt(text, caret)),
            )

            val module = server.context.projectStructureState.useSiteModuleForOpenDocument(uri)
                ?: error("文档 $uri 未绑定 use-site 模块，实验前提不成立")

            // 光标停在半截标识符之后；平台在 IDE 侧会在副本里插入这个标记，LSP 侧必须自己插。
            val copyText = StringBuilder(text).insert(caret, DUMMY_MARKER).toString()
            val copy = LspAnalysisPsiFileFactory.createFile(project, uri, USE_FILE, copyText)

            // 修正是一次 PSI 写：必须在 EDT 上的写命令内执行。
            // 直接在本线程调 `CommandProcessor.executeCommand` 会**静默不执行**（实测：捕获变量仍为 null），
            // 因此照仓库既有夹具的写法：EDT + `WriteCommandAction.runWriteCommandAction`。
            var request: PlatformCompletionRequest? = null
            java.awt.EventQueue.invokeAndWait {
                WriteCommandAction.runWriteCommandAction(project, "probe: dummy correction", null, Runnable {
                    val dummy = CompletionDummyIdentifierProvider.provideFor(copy, caret)
                        ?: error("dummy 修正在 $caret 处返回 null：该位置被判定为不可补全")
                    request = PlatformCompletionRequest(
                        originalPosition = dummy.position,
                        completionPosition = dummy.position,
                        prefix = dummy.prefix,
                        kind = CompletionKind.BASIC,
                        invocationCount = 1,
                        offset = caret,
                        replaceStartOffset = caret - dummy.prefix.length,
                        replaceEndOffset = caret,
                        documentStamp = 1L,
                        positionElement = dummy.position,
                    )
                })
            }

            val service = cfirCompletionServiceOrNull(project)
                ?: error("补全服务未注册：LSP 容器缺少 CfirCompletionServiceProvider")
            /*
             * 服务入口要求有进度指示器：RPC 线程与测试线程都没有，
             * 直接调会抛 `getProgressIndicator(...) must not be null`。
             * 真实实现必须自己开一个（这是服务端侧的一条硬约束，不是测试特例）。
             */
            val snapshots = analyze(module) {
                ProgressManager.getInstance().runProcess(
                    Computable { service.complete(requireNotNull(request)) },
                    EmptyProgressIndicator(),
                )
            }

            assertTrue(
                snapshots.any { it.lookupString == "buildUser" },
                "修正后的副本必须能给出候选（本实验判定副本形态是否可行）。" +
                    "实际候选 ${snapshots.map { it.lookupString }}；" +
                    "修正在副本上得到的文件是 ${request?.completionPosition?.containingCjFile?.name}",
            )
        }
    }

    /**
     * 把文本偏移换算成 LSP 位置（本实验的夹具源码只用 `\n`）。
     */
    private fun positionAt(text: String, offset: Int): Position {
        val prefix = text.substring(0, offset)
        val line = prefix.count { it == '\n' }
        val lineStart = prefix.lastIndexOf('\n') + 1
        return Position(line, offset - lineStart)
    }

    private companion object {
        /** 供补全的顶层函数所在文件（夹具把 `source(name)` 写到 `src/<name>`）。 */
        const val MODEL_FILE = "src/model.cj"

        /** 触发补全的位置所在文件。 */
        const val USE_FILE = "src/use.cj"

        /** `source(...)` 使用的文件名。 */
        const val MODEL_NAME = "model.cj"

        /** `source(...)` 使用的触发文件名。 */
        const val USE_NAME = "use.cj"

        /** 半截标识符：用户刚敲到一半就触发补全。 */
        const val PARTIAL_NAME = "buil"

        /** 平台 dummy 标记；与 `CompletionDummyIdentifierProvider` 内部约定一致。 */
        const val DUMMY_MARKER = "IntellijIdeaRulezzz"

        /** 依赖文件源码。 */
        val MODEL_SOURCE = """
            package sample.probe

            func buildUser(): Int64 {
                1
            }
        """.trimIndent()

        /** 触发文件源码：`let u = buil` 是半截标识符，语法上不成立，正是需要 dummy 修正的场景。 */
        val USE_SOURCE = """
            package sample.probe

            func consume(): Int64 {
                let u = buil
                return 1
            }
        """.trimIndent()
    }
}
