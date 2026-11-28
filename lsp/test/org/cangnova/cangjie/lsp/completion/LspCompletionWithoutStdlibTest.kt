package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 无 SDK / 无库模块会话下的补全基线（任务 13.1 的「无 SDK 基线」）。
 *
 * 要求只有一条，但它是**行为规格**而不是配置形态：这种会话里**不得退化为全工作区声明遍历**。
 * 夹具因此故意造得能抓住这种退化——工作区里放 50 个生成文件，每个带一个唯一的顶层函数名；
 * 它们既没被导入、也不在使用点作用域里，**只能**靠索引找到：
 * - 本容器不建索引（同 `CfirIndexDegradationTest` 的前提），所以它们**必须缺席**；
 * - 若哪天它们出现了，只有两种解释，都必须由人判断：
 *   ①容器开始建索引了（把断言改成索引语义），②有人加了全工作区扫描兜底（13.1 明令禁止）。
 * 反向断言作用域内候选仍在——降级是「少给」，不是「不给」。
 */
class LspCompletionWithoutStdlibTest : AbstractLspIntegrationTest() {
    /**
     * 本类自建会话。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 无 stdlib 时：作用域内候选照常，工作区其它文件的顶层声明不得出现。
     */
    @Test
    fun workspaceDeclarationsAreNotScannedWithoutStdlib() {
        val builder = LspWorkspaceFixtureBuilder().withoutStdlib()
        repeat(GENERATED_FILES) { index ->
            builder.source(
                "generated/Gen$index.cj",
                "package generated" + NL + NL + "func generated$index(): Int64 { return $index }" + NL,
            )
        }
        val fixture = builder.source(USE_NAME, USE_SOURCE).build()

        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            val uri = fixture.uri(USE_FILE)
            val text = fixture.text(USE_FILE)
            connection.openDocument(uri, text)

            val items = connection.completion(
                CompletionParams(TextDocumentIdentifier(uri), positionAt(text)),
            ).left
            val names = items.mapNotNull { it.label }.toSet()
            println("CJD-NOSDK 候选数=${names.size} 样例=" + names.take(12))

            val scanned = names.filter { it.startsWith("generated") }
            assertTrue(
                scanned.isEmpty(),
                "无索引时不得出现其它文件的顶层声明（它们只能来自全工作区扫描）：$scanned",
            )
            assertTrue(
                names.contains("localHelper"),
                "作用域内声明仍须可补全（降级是少给，不是不给），实际 $names",
            )
        }
    }

    companion object {
        /** 行尾。 */
        private const val NL = "\n"

        /** 使用点文件的源根相对名。 */
        private const val USE_NAME = "use.cj"

        /** 使用点文件的工作区相对路径（含 `src/`）。 */
        private const val USE_FILE = "src/use.cj"

        /** 使用点源码：位置与前缀确定，作用域内有一个本文件声明可供补全。 */
        private const val USE_SOURCE = "package sample.nosdk" + NL + NL +
            "func localHelper(): Int64 { return 0 }" + NL + NL +
            "func consume(): Int64 {" + NL + "    let u =" + NL + "    return 1" + NL + "}" + NL

        /** 工作区里生成的「干扰文件」数量。 */
        private const val GENERATED_FILES = 50

        /**
         * 补全位置：`let u =` 之后（与统计、基线用例同一口径）。
         */
        private fun positionAt(text: String): Position {
            val offset = text.indexOf("let u =") + "let u =".length
            val prefix = text.substring(0, offset)
            return Position(prefix.count { it == '\n' }, offset - (prefix.lastIndexOf('\n') + 1))
        }
    }
}
