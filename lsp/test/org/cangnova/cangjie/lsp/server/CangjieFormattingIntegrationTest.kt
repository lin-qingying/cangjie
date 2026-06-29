package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * 校验 LSP 文档格式化请求与共享格式化器的集成行为。
 *
 * 该测试通过真实 LSP 会话发送 formatting 请求，确认格式化结果使用客户端提供的缩进宽度。
 */
class CangjieFormattingIntegrationTest : AbstractLspIntegrationTest() {
    /**
     * 校验整篇文档格式化会返回覆盖全文的单个编辑。
     *
     * 该用例固定函数空格、运算符空格和两空格缩进，确保 LSP 层正确透传 `FormattingOptions`。
     */
    @Test
    fun `document formatting returns shared formatter edit and respects indent width`() {
        val uri = "file:///workspace/formatting.cj"
        session.openDocument(
            uri,
            """
            func main(){
            let value=1+2
            }
            """.trimIndent(),
        )

        val edits = session.formatting(
            DocumentFormattingParams(
                TextDocumentIdentifier(uri),
                FormattingOptions(2, true),
            ),
        )

        assertFalse(edits.isEmpty())
        assertEquals(1, edits.size)
        assertEquals(Position(0, 0), edits.single().range.start)
        assertEquals(
            """
            func main() {
              let value = 1 + 2
            }
            """.trimIndent(),
            edits.single().newText.trimEnd(),
        )
    }
}
