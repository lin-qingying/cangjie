package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.eclipse.lsp4j.CompletionItemKind as LspCompletionItemKind

/**
 * 把仓颉候选映射成 LSP 的 `CompletionItemKind`。
 *
 * 刻意**不一律降级为 `Text`**：客户端据此选图标与分组，全 `Text` 会让候选列表失去可读性
 * （旧实现正是这样，见设计 §7.11.5）。
 *
 * 判定只用快照里确实冻结了的事实，不猜：
 * - [CompletionSnapshot.kind]：候选是什么形状（局部/成员/顶层/关键字/…）；
 * - [CompletionSnapshot.identity]`.kind`：是类型、是符号，还是关键字/包/文档这类非符号；
 * - [CompletionSnapshot.tailText]：签名渲染文本，可调用候选以 `(` 开头（与 IDE 侧同一判据）。
 *
 * 快照无法回答的细分类别（仓颉的 `class`/`interface`/`struct`/`enum`，成员是字段还是属性）
 * 一律取 LSP 的中性项并在下文注明——宁可给中性类别，也不按名字或文本去猜。
 */
object LspCompletionItemKindProvider {
    /**
     * 取候选在 LSP 协议下的类别。
     */
    fun kindOf(snapshot: CompletionSnapshot): LspCompletionItemKind =
        when (snapshot.identity.kind) {
            // 身份说是「类型」但没说哪一类：Class 是中性选择，客户端另有签名可读。
            CompletionIdentityKind.TYPE -> LspCompletionItemKind.Class

            CompletionIdentityKind.PACKAGE -> LspCompletionItemKind.Module
            CompletionIdentityKind.KEYWORD -> LspCompletionItemKind.Keyword
            CompletionIdentityKind.NAMED_ARGUMENT -> LspCompletionItemKind.Property
            CompletionIdentityKind.ENUM_ENTRY -> LspCompletionItemKind.EnumMember
            CompletionIdentityKind.DECLARATION_SUGGESTION -> LspCompletionItemKind.Snippet
            // 文档标签/引用不是语言符号，LSP 无对应类别：`Text` 在这里是**准确**而不是降级。
            CompletionIdentityKind.DOCUMENTATION -> LspCompletionItemKind.Text

            CompletionIdentityKind.SYMBOL -> symbolKindOf(snapshot)
        }

    /**
     * 符号候选的细分类别。
     *
     * 形状与签名两维合用：局部/成员/顶层决定「函数还是值」，
     * 签名文本（可调用候选以 `(` 开头）决定同一族里的具体项。
     * 仓颉的成员既可写字段也可写 `prop`，快照不区分，故成员的非调用形态取 `Field`。
     */
    private fun symbolKindOf(snapshot: CompletionSnapshot): LspCompletionItemKind = when (snapshot.kind) {
        CompletionItemKind.LOCAL -> LspCompletionItemKind.Variable
        CompletionItemKind.TYPE_PARAMETER -> LspCompletionItemKind.TypeParameter
        CompletionItemKind.MEMBER,
        CompletionItemKind.STATIC_MEMBER,
        CompletionItemKind.EXTEND_MEMBER,
        CompletionItemKind.IMPORT_MEMBER,
        -> if (snapshot.isCallableShape) LspCompletionItemKind.Method else LspCompletionItemKind.Field

        CompletionItemKind.TOP_LEVEL ->
            if (snapshot.isCallableShape) LspCompletionItemKind.Function else LspCompletionItemKind.Variable

        /*
         * 其余形状不该以 SYMBOL 身份出现（身份与形状由同一批候选构造同时给出）。
         * 真出现时不假装认得：给 `Text` 并留下中性结果，比按形状硬套一个错类别好。
         */
        else -> LspCompletionItemKind.Text
    }

    /**
     * 候选是否呈调用形态。
     *
     * 判据取签名文本的首字符：可调用候选的签名渲染以参数列表开头（`(a: A): R`），
     * 值的类型文本不带括号。空签名按「不是调用」处理——它是值而不是函数。
     */
    private val CompletionSnapshot.isCallableShape: Boolean
        get() = tailText.startsWith("(")
}
