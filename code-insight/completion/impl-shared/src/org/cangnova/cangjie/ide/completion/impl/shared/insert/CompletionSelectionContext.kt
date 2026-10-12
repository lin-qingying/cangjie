package org.cangnova.cangjie.ide.completion.impl.shared.insert

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.ide.completion.impl.shared.lookup.COMPLETION_SNAPSHOT

/**
 * 选中候选前捕获的文档版本。
 *
 * IntelliJ 在 `beforeItemSelected` 回调之后才替换查找串。仓颉插入处理器需要这个戳，
 * 才能把平台自己的替换与补全请求后发生的真实编辑区分开。
 */
val COMPLETION_SELECTION_DOCUMENT_STAMP: Key<Long> =
    Key.create("CANGJIE_COMPLETION_SELECTION_DOCUMENT_STAMP")

/**
 * 记录一次仓颉候选选择所对应的文档版本，并判定平台是否可以继续插入查找串。
 */
object CompletionSelectionContext {
    /**
     * 在平台替换查找串前捕获文档戳。
     *
     * 返回 `false` 时表示快照已过期，调用方应阻止 IntelliJ 写入查找串；非仓颉候选
     * 不带快照，直接放行且不修改其用户数据。
     */
    fun beforeItemSelected(item: LookupElement, document: Document): Boolean {
        val snapshot = item.getUserData(COMPLETION_SNAPSHOT) ?: return true
        val selectionStamp = document.modificationStamp
        item.forEachLookupElementDelegate { element ->
            element.putUserData(COMPLETION_SELECTION_DOCUMENT_STAMP, selectionStamp)
        }
        return InsertionValidator.validateDocumentStamp(snapshot, selectionStamp).writable
    }

    /**
     * 读取插入处理器实际收到的元素或其 lookup decorator 上的选中版本。
     *
     * IntelliJ 的优先级、排序等包装器会在查找阶段装饰候选；平台可能把装饰元素或
     * 被装饰元素传给插入处理器。捕获时写入整条委托链，读取时按同一条链查找，确保
     * `beforeItemSelected` 的上下文能跨过平台包装器到达插入处理器。
     */
    fun selectionStampOf(item: LookupElement): Long? {
        var selectionStamp: Long? = null
        item.forEachLookupElementDelegate { element ->
            if (selectionStamp == null) {
                selectionStamp = element.getUserData(COMPLETION_SELECTION_DOCUMENT_STAMP)
            }
        }
        return selectionStamp
    }

    private inline fun LookupElement.forEachLookupElementDelegate(action: (LookupElement) -> Unit) {
        var current: LookupElement? = this
        while (current != null) {
            action(current)
            current = (current as? LookupElementDecorator<*>)?.delegate
        }
    }
}
