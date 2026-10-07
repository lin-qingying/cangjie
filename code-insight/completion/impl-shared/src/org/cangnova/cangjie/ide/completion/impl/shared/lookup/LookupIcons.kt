package org.cangnova.cangjie.ide.completion.impl.shared.lookup

import com.intellij.icons.AllIcons
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import javax.swing.Icon

/**
 * 候选类别到平台图标的映射。
 *
 * 图标是**展示数据**，必须在分析期确定：界面期已经拿不到 Analysis，
 * 也不应该为了取一个图标重新解析符号。
 *
 * 只用平台自带图标集，不引入仓颉专属图标资源：
 * 补全层先保证语义正确，图标资源属于 UX 收尾，另行接入。
 */
internal fun iconOf(kind: CompletionItemKind): Icon = when (kind) {
    CompletionItemKind.LOCAL -> AllIcons.Nodes.Variable
    // 类型参数是分类器不是值：复用 Parameter 图标，与 NAMED_ARGUMENT 的「实参名」区分形状语义。
    CompletionItemKind.TYPE_PARAMETER -> AllIcons.Nodes.Parameter
    CompletionItemKind.MEMBER -> AllIcons.Nodes.Method
    CompletionItemKind.STATIC_MEMBER -> AllIcons.Nodes.Static
    CompletionItemKind.EXTEND_MEMBER -> AllIcons.Nodes.AbstractClass
    CompletionItemKind.TOP_LEVEL -> AllIcons.Nodes.Class
    CompletionItemKind.PACKAGE -> AllIcons.Nodes.Package
    CompletionItemKind.IMPORT_MEMBER -> AllIcons.Nodes.Related
    CompletionItemKind.KEYWORD -> AllIcons.Nodes.Static
    CompletionItemKind.NAMED_ARGUMENT -> AllIcons.Nodes.Parameter
    CompletionItemKind.ENUM_ENTRY -> AllIcons.Nodes.Enum
    CompletionItemKind.DOCUMENTATION -> AllIcons.Actions.Edit
    // 声明生成建议插入的是**新声明**而不是一次引用：用「新增」图标与真实候选区分开，
    // 否则用户会以为选中它就引用了某个已有成员。
    CompletionItemKind.DECLARATION_SUGGESTION -> AllIcons.General.Add
}