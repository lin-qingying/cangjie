package org.cangnova.cangjie.lsp.completion

import com.google.gson.JsonPrimitive
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind as LspCompletionItemKind
import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * LSP 候选字段映射回归（设计 §7.11.3）。
 *
 * 这一层的存在理由是「两端呈现一致」这条验收：字段映射错位的表现是**静默的**——
 * 客户端仍会列出候选，只是图标全一样、签名看不到、选中后什么都没发生。
 * 因此逐字段锁死映射，而不是靠端到端测试顺带覆盖。
 */
class LspCompletionItemConverterTest {
    /**
     * 顶层可调用候选：标签取展示名，签名进 `labelDetails.detail`，类别为函数。
     */
    @Test
    fun topLevelCallableMapsToFunctionWithSignatureDetail() {
        val item = convert(
            snapshot(
                lookupString = "buildUser",
                kind = CompletionItemKind.TOP_LEVEL,
                identityKind = CompletionIdentityKind.SYMBOL,
                tailText = "(name: String): User",
                typeText = "User",
            ),
        )

        assertEquals("buildUser", item.label, "标签取快照的展示名")
        assertEquals(LspCompletionItemKind.Function, item.kind, "带参数列表的顶层符号是函数")
        assertEquals("(name: String): User", item.labelDetails.detail, "签名进 labelDetails.detail")
        assertEquals("User", item.labelDetails.description, "返回类型进 labelDetails.description")
    }

    /**
     * 类型候选：身份说「类型」时给 Class，而不是按签名形状猜成函数。
     */
    @Test
    fun typeIdentityMapsToClass() {
        val item = convert(
            snapshot(lookupString = "Widget", kind = CompletionItemKind.TOP_LEVEL, identityKind = CompletionIdentityKind.TYPE),
        )

        assertEquals(LspCompletionItemKind.Class, item.kind)
    }

    /**
     * 关键字与包各有类别，**不得**一律降级为 `Text`（旧实现正是这样）。
     */
    @Test
    fun keywordAndPackageAreNotFlattenedToText() {
        val keyword = convert(
            snapshot(lookupString = "if", kind = CompletionItemKind.KEYWORD, identityKind = CompletionIdentityKind.KEYWORD),
        )
        val packageItem = convert(
            snapshot(lookupString = "std.core", kind = CompletionItemKind.PACKAGE, identityKind = CompletionIdentityKind.PACKAGE),
        )

        assertEquals(LspCompletionItemKind.Keyword, keyword.kind)
        assertEquals(LspCompletionItemKind.Module, packageItem.kind)
    }

    /**
     * 成员的「函数还是值」由签名形状决定：没有签名文本的就是值。
     */
    @Test
    fun memberWithoutSignatureIsAValue() {
        val value = convert(snapshot(lookupString = "size", kind = CompletionItemKind.MEMBER))
        val callable = convert(
            snapshot(lookupString = "size", kind = CompletionItemKind.MEMBER, tailText = "(): Int64"),
        )

        assertEquals(LspCompletionItemKind.Field, value.kind)
        assertEquals(LspCompletionItemKind.Method, callable.kind)
    }

    /**
     * 没有签名与类型时不设 `labelDetails` 字段，而不是塞空串——
     * 空串会让客户端渲染出空的括号区。
     */
    @Test
    fun emptySignatureLeavesLabelDetailsFieldsUnset() {
        val item = convert(snapshot(lookupString = "counter", kind = CompletionItemKind.LOCAL))

        assertNull(item.labelDetails, "无签名无类型时不设 labelDetails")
    }

    /**
     * 排序文本按列表下标零填充：客户端做字符串比较也必须得到服务端顺序。
     */
    @Test
    fun sortTextEncodesServerSideOrder() {
        val zero = LspCompletionSortingUtil.sortTextOf(0)
        val nine = LspCompletionSortingUtil.sortTextOf(9)
        val ten = LspCompletionSortingUtil.sortTextOf(10)

        assertTrue(zero < nine && nine < ten, "字符串序必须与数值序一致，实际 $zero/$nine/$ten")
        assertEquals(zero, convert(snapshot(lookupString = "a"), index = 0).sortText)
        assertEquals(ten, convert(snapshot(lookupString = "b"), index = 10).sortText)
    }

    /**
     * 插入机制：空编辑 + 命令。
     *
     * 编辑必须是空的（真正改动由服务端经 `workspace/applyEdit` 发起），
     * 命令实参按约定携带会话键与下标，客户端只负责原样回传。
     */
    @Test
    fun itemCarriesEmptyEditAndApplyCommand() {
        val item = convert(snapshot(lookupString = "buildUser"), sessionKey = "session-7", index = 3)
        val edit = item.textEdit.left

        assertEquals("", edit.newText, "候选自带的编辑必须是空的")
        assertEquals(edit.range.start, edit.range.end, "空编辑的范围是零长区间")
        assertEquals(CARET, edit.range.start, "空编辑落在光标处")
        assertEquals(LspCompletionCommands.APPLY_COMPLETION, item.command.command)
        assertEquals(listOf("session-7", 3), item.command.arguments)
        assertEquals("session-7", (item.data as JsonPrimitive).asString, "data 携带会话键")
    }

    /**
     * 转换使用的候选。
     */
    private fun snapshot(
        lookupString: String,
        kind: CompletionItemKind = CompletionItemKind.TOP_LEVEL,
        identityKind: CompletionIdentityKind = CompletionIdentityKind.SYMBOL,
        tailText: String = "",
        typeText: String = "",
    ): CompletionSnapshot = completionSnapshot(
        lookupString = lookupString,
        kind = kind,
        origin = CompletionOrigin.TOP_LEVEL,
        identityKind = identityKind,
        tailText = tailText,
        typeText = typeText,
    ).build()

    /**
     * 按测试默认参数转换。
     */
    private fun convert(
        snapshot: CompletionSnapshot,
        index: Int = 0,
        sessionKey: String = "session-1",
    ): CompletionItem = LspCompletionItemConverter.toCompletionItem(snapshot, index, CARET, sessionKey)

    private companion object {
        /** 测试光标位置。 */
        val CARET = Position(3, 7)
    }
}
