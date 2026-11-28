package org.cangnova.cangjie.lsp.completion

import com.intellij.openapi.components.service
import org.cangnova.cangjie.ide.completion.api.statistics.CangJieCompletionStatistics
import org.cangnova.cangjie.ide.completion.api.statistics.CompletionRejection
import org.cangnova.cangjie.ide.completion.api.statistics.CompletionStage
import org.cangnova.cangjie.ide.completion.api.statistics.RecordingCompletionStatisticsSink
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

/**
 * 补全统计的接线回归（设计 §7.10：阶段耗时、候选量、索引查询、取消/过期计数）。
 *
 * 统计最容易出的问题是**静默不生效**：埋点漏了一处、服务没注册、实现被替换成 noop——
 * 三种情况都不报错，只是面板上没有数据。因此这里用**记录式实现**断言「该记的都记了」：
 * 把 sink 换成记录实现，跑一次真实补全与一次过期会话，逐项检查。
 */
class LspCompletionStatisticsTest : AbstractLspIntegrationTest() {
    /**
     * 本类自建连接与工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 一次成功的补全：候选量、候选阶段、索引查询阶段、渲染阶段都要有记录。
     */
    @Test
    fun successfulCompletionRecordsCandidateAndStageMetrics() {
        withServer { connection, fixture ->
            val recorder = installRecordingSink(connection)
            val uri = fixture.uri(USE_FILE)
            connection.openDocument(uri, fixture.text(USE_FILE))

            val items = connection.completion(
                CompletionParams(TextDocumentIdentifier(uri), positionAt(fixture.text(USE_FILE))),
            ).left

            assertTrue(items.isNotEmpty(), "前置条件：该位置本来就有候选，否则统计断言没有意义")
            assertTrue(
                recorder.lastProduced >= recorder.lastDelivered,
                "产出数必须不少于交付数，实际 produced=${recorder.lastProduced} delivered=${recorder.lastDelivered}",
            )
            assertTrue(recorder.lastDelivered > 0, "交付数必须被记录，实际 ${recorder.lastDelivered}")
            assertTrue(
                recorder.stageCounts.containsKey(CompletionStage.CANDIDATES),
                "候选阶段必须被计时，实际 ${recorder.stageCounts}",
            )
            assertTrue(
                recorder.stageCounts.containsKey(CompletionStage.RENDER),
                "渲染阶段必须被计时（LSP 侧把快照转成 CompletionItem 的那一步），实际 ${recorder.stageCounts}",
            )
            assertTrue(
                recorder.stageCounts.containsKey(CompletionStage.SETUP),
                "准备阶段（副本与 dummy 修正）必须被计时，实际 ${recorder.stageCounts}",
            )
            assertTrue(
                recorder.stageNanos.values.all { it >= 0 },
                "耗时不得为负，实际 ${recorder.stageNanos}",
            )
        }
    }

    /**
     * 过期会话：必须记一笔 `SESSION_EXPIRED`，而不是只报错。
     */
    @Test
    fun expiredSessionRecordsRejectionReason() {
        withServer { connection, fixture ->
            val recorder = installRecordingSink(connection)
            val uri = fixture.uri(USE_FILE)
            connection.openDocument(uri, fixture.text(USE_FILE))

            runCatching {
                connection.executeCommand(
                    ExecuteCommandParams(LspCompletionCommands.APPLY_COMPLETION, listOf("cj-completion-404", 0)),
                )
            }.onFailure { failure ->
                assertTrue(failure is ExecutionException, "过期会话应当以协议错误返回，实际 $failure")
            }

            assertEquals(
                1,
                recorder.rejections[CompletionRejection.SESSION_EXPIRED] ?: 0,
                "过期会话必须记一笔 SESSION_EXPIRED，实际 ${recorder.rejections}",
            )
        }
    }

    /**
     * `completionItem/resolve` 必须**单独**计时。
     *
     * resolve 是用户按下候选才触发的第二次分析（按身份重进分析、取回文档），
     * 它的耗时直接决定「翻候选卡不卡」。混进候选生成就看不出来，漏埋点同样不报错——
     * 所以这里用记录式 sink 直接断言这一阶段存在。
     */
    @Test
    fun resolveRecordsItsOwnStage() {
        withServer { connection, fixture ->
            val recorder = installRecordingSink(connection)
            val uri = fixture.uri(USE_FILE)
            connection.openDocument(uri, fixture.text(USE_FILE))
            val item = connection.completion(
                CompletionParams(TextDocumentIdentifier(uri), positionAt(fixture.text(USE_FILE))),
            ).left.firstOrNull() ?: error("前置条件：该位置本来就有候选")

            connection.resolveCompletionItem(item)

            assertTrue(
                recorder.stageCounts.containsKey(CompletionStage.RESOLVE),
                "resolve 必须单独计时（与候选生成分开），实际 ${recorder.stageCounts}",
            )
        }
    }

    /**
     * 把工程的统计实现换成记录式。
     */
    private fun installRecordingSink(connection: LspIntegrationTestConnection): RecordingCompletionStatisticsSink {
        val recorder = RecordingCompletionStatisticsSink()
        connection.server.context.environment.project
            .service<CangJieCompletionStatistics>()
            .sink = recorder
        return recorder
    }

    /**
     * 造夹具、开会话。
     */
    private fun withServer(body: (LspIntegrationTestConnection, LspWorkspaceFixture) -> Unit) {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(USE_NAME, USE_SOURCE)
            .build()
        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            body(connection, fixture)
        }
    }

    /**
     * 偏移 → LSP 位置（夹具源码只用 `\n`）；位置取 `let u =` 之后（空前缀）。
     */
    private fun positionAt(text: String): Position {
        val offset = text.indexOf("let u =") + "let u =".length
        val prefix = text.substring(0, offset)
        return Position(prefix.count { it == '\n' }, offset - (prefix.lastIndexOf('\n') + 1))
    }

    private companion object {
        const val USE_NAME = "use.cj"
        const val USE_FILE = "src/use.cj"

        /** 顶层函数可供补全。 */
        val USE_SOURCE = """
            package sample.statistics

            func buildUser(): Int64 {
                1
            }

            func consume(): Int64 {
                let u =
                return 1
            }
        """.trimIndent()
    }
}
