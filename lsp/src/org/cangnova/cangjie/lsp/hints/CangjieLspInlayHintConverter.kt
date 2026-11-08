package org.cangnova.cangjie.lsp.hints

import org.cangnova.cangjie.codeinsight.hints.CangJieInlayHint
import org.cangnova.cangjie.codeinsight.hints.CangJieInlayHintKind
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintKind

/**
 * 共享 inlay hint 到 LSP 协议对象的转换。
 *
 * 协议侧只消费纯文本：lsp4j 1.0.0 的 `InlayHintCapabilities` 没有 `labelKindSupport`，
 * 片段化的 `InlayHintLabelPart` 无法保证被客户端理解，因此标签一律折叠成单个 `String`，
 * 共享层里 IDE 专用的 `target` 指针在这里整体丢弃。
 */
object CangjieLspInlayHintConverter {
    /**
     * 把一条共享提示转换为 LSP 协议对象。
     */
    fun toInlayHint(document: LspTextDocument, hint: CangJieInlayHint): InlayHint {
        val position = document.analysisPositionAt(hint.offset)
        return InlayHint().apply {
            setPosition(position)
            setLabel(hint.plainText)
            kind = hint.kind.toLspKind()
            setPaddingLeft(hint.paddingLeft)
            setPaddingRight(hint.paddingRight)
            hint.tooltip?.let { setTooltip(it) }
        }
    }

    /**
     * 共享类别到协议类别的映射；`OTHER` 在协议里没有对应值，按「未分类」省略。
     */
    private fun CangJieInlayHintKind.toLspKind(): InlayHintKind? = when (this) {
        CangJieInlayHintKind.TYPE -> InlayHintKind.Type
        CangJieInlayHintKind.PARAMETER_NAME -> InlayHintKind.Parameter
        CangJieInlayHintKind.OTHER -> null
    }
}
