package org.cangnova.cangjie.lsp.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.cangnova.cangjie.ide.completion.impl.shared.lookup.LookupElementFactory
import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * 「同一条快照在 IDE 与 LSP 两端渲染一致」的机械对照（任务 15.5 的 IDE 侧对照一半）。
 *
 * 两端各有一份独立的渲染实现——IDE 侧 [LookupElementFactory]，LSP 侧
 * [LspCompletionItemConverter]——此前只有各自的单端用例（`LookupElementFactoryTest`
 * 与 `LspCompletionItemConverterTest`），**同一份快照经两端渲染是否给出同样的文字**
 * 没有任何断言。字段漂移的症状是静默的：两端各自看起来都对，只有并排比才看得出
 * 签名少了一段、类型落到别处。这条对照就是把「两端一致」从口头验收变成机械判据。
 */
class LspIdeTwoEndPresentationTest {
    /**
     * 主文本、签名、类型三项必须两端同值。
     *
     * 两端各自的来源字段不同（IDE 走 `presentableText` + `tailText` + `typeText`，
     * LSP 走 `label` + `labelDetails.detail/description`），因此这里比的是**渲染结果**，
     * 而不是各自的字段名——字段名一致说明不了用户看到的一致。
     */
    @Test
    fun bothEndsRenderTheSameSnapshotIdentically() {
        val snapshot = completionSnapshot(
            lookupString = "buildUser",
            kind = CompletionItemKind.TOP_LEVEL,
            origin = CompletionOrigin.TOP_LEVEL,
            identityKind = CompletionIdentityKind.SYMBOL,
            tailText = "(name: String): User",
            typeText = "User",
        ).build()

        val lspItem = LspCompletionItemConverter.toCompletionItem(
            snapshot,
            index = 0,
            caret = Position(0, 0),
            sessionKey = "session",
        )
        val idePresentation = presentationOf(LookupElementFactory.create(snapshot))

        assertEquals(lspItem.label, idePresentation.itemText, "两端主文本必须一致")
        assertEquals(lspItem.labelDetails?.detail, idePresentation.tailText, "两端签名尾文本必须一致")
        assertEquals(lspItem.labelDetails?.description, idePresentation.typeText, "两端类型文本必须一致")
        assertNotNull(idePresentation.icon, "IDE 侧候选必须带图标（LSP 侧由 kind 决定客户端图标）")
    }

    /**
     * 没有签名与类型的候选，两端都**不得**凭空造出括号区。
     *
     * LSP 侧的空 `labelDetails` 与 IDE 侧的空尾/类型文本是同一个约定的两端：
     * 一边不设、另一边写了空串，客户端就会渲染出多余的空括号或空类型位。
     */
    @Test
    fun bothEndsOmitEmptyTailAndType() {
        val snapshot = completionSnapshot(
            lookupString = "visible",
            kind = CompletionItemKind.MEMBER,
            origin = CompletionOrigin.MEMBER,
            tailText = "",
            typeText = "",
        ).build()

        val lspItem = LspCompletionItemConverter.toCompletionItem(
            snapshot,
            index = 0,
            caret = Position(0, 0),
            sessionKey = "session",
        )
        val idePresentation = presentationOf(LookupElementFactory.create(snapshot))

        assertEquals(null, lspItem.labelDetails, "LSP 侧签名与类型都为空时整体不设 labelDetails")
        assertEquals("", idePresentation.tailText.orEmpty(), "IDE 侧尾文本必须为空")
        assertEquals("", idePresentation.typeText.orEmpty(), "IDE 侧类型文本必须为空")
        assertEquals(lspItem.label, idePresentation.itemText, "主文本仍须一致")
    }

    /**
     * 读取元素呈现：builder 的尾文本/类型文本由 expensive renderer 渲染，
     * 普通 `renderElement` 只写主文本。
     */
    private fun presentationOf(element: LookupElement): LookupElementPresentation {
        val presentation = LookupElementPresentation()
        val renderer = expensiveRendererOf(element)
        if (renderer != null) {
            renderer.renderElement(element, presentation)
        } else {
            element.renderElement(presentation)
        }
        return presentation
    }

    /**
     * 取得元素的 expensive renderer；装饰器没有自己的渲染器时沿委托链向下找。
     */
    private fun expensiveRendererOf(element: LookupElement): LookupElementRenderer<LookupElement>? {
        val decorator = element as? LookupElementDecorator<*> ?: return null
        @Suppress("UNCHECKED_CAST")
        val own = decorator.expensiveRenderer as? LookupElementRenderer<LookupElement>
        return own ?: decorator.delegate.let(::expensiveRendererOf)
    }
}
