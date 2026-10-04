package org.cangnova.cangjie.ide.completion.impl.shared.lookup

import com.intellij.icons.AllIcons
import org.cangnova.cangjie.ide.completion.api.CaCompletionItemKind
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
internal fun iconOf(kind: CaCompletionItemKind): Icon = when (kind) {
    CaCompletionItemKind.LOCAL -> AllIcons.Nodes.Variable
    CaCompletionItemKind.MEMBER -> AllIcons.Nodes.Method
    CaCompletionItemKind.STATIC_MEMBER -> AllIcons.Nodes.Static
    CaCompletionItemKind.EXTEND_MEMBER -> AllIcons.Nodes.AbstractClass
    CaCompletionItemKind.TOP_LEVEL -> AllIcons.Nodes.Class
    CaCompletionItemKind.PACKAGE -> AllIcons.Nodes.Package
    CaCompletionItemKind.IMPORT_MEMBER -> AllIcons.Nodes.Related
    CaCompletionItemKind.KEYWORD -> AllIcons.Nodes.Static
    CaCompletionItemKind.NAMED_ARGUMENT -> AllIcons.Nodes.Parameter
    CaCompletionItemKind.ENUM_ENTRY -> AllIcons.Nodes.Enum
    CaCompletionItemKind.DOCUMENTATION -> AllIcons.Actions.Edit
}