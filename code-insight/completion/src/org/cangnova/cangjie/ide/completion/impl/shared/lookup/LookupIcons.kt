package org.cangnova.cangjie.ide.completion.impl.shared.lookup

import com.intellij.icons.AllIcons
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionSymbolKind
import javax.swing.Icon

/**
 * 候选类别 + 声明形态到平台图标的映射。
 *
 * 图标是**展示数据**，必须在分析期确定：界面期已经拿不到 Analysis，
 * 也不应该为了取一个图标重新解析符号。
 *
 * 只用平台自带图标集，不引入仓颉专属图标资源：
 * 补全层先保证语义正确，图标资源属于 UX 收尾，另行接入。
 *
 * 按 [CompletionSymbolKind] 分派而不是只看类别：顶层一类里同时有 class、interface、
 * struct、enum、typealias 与函数，只按类别取图标会让它们共用一个 class 图标，
 * 用户在弹窗里无从区分。形态未定（[CompletionSymbolKind.OTHER]）时退回按类别映射。
 */
internal fun iconOf(kind: CompletionItemKind, symbolKind: CompletionSymbolKind): Icon = when (symbolKind) {
    CompletionSymbolKind.CLASS -> AllIcons.Nodes.Class
    CompletionSymbolKind.INTERFACE -> AllIcons.Nodes.Interface
    // 平台没有 struct 图标：值类型聚合取 record 形态，与 class 的可继承语义区分开。
    CompletionSymbolKind.STRUCT -> AllIcons.Nodes.Record
    CompletionSymbolKind.ENUM -> AllIcons.Nodes.Enum
    CompletionSymbolKind.TYPE_ALIAS -> AllIcons.Nodes.Alias
    // 同一形态在成员位置与方法位置用不同图标：平台本身就把 Method 与 Function 分开画。
    CompletionSymbolKind.FUNCTION ->
        if (kind.isMemberLike()) AllIcons.Nodes.Method else AllIcons.Nodes.Function

    CompletionSymbolKind.PROPERTY -> AllIcons.Nodes.Property
    CompletionSymbolKind.FIELD -> AllIcons.Nodes.Field
    CompletionSymbolKind.VARIABLE -> AllIcons.Nodes.Variable
    // 类型参数是分类器不是值：复用 Parameter 图标，与 NAMED_ARGUMENT 的「实参名」区分形状语义。
    CompletionSymbolKind.PARAMETER -> AllIcons.Nodes.Parameter
    CompletionSymbolKind.TYPE_PARAMETER -> AllIcons.Nodes.Parameter
    CompletionSymbolKind.CONSTRUCTOR -> AllIcons.Nodes.Constructor
    CompletionSymbolKind.ENUM_CONSTRUCTOR -> AllIcons.Nodes.Enum
    CompletionSymbolKind.PACKAGE -> AllIcons.Nodes.Package
    CompletionSymbolKind.KEYWORD -> AllIcons.Nodes.Static
    CompletionSymbolKind.OTHER -> iconOfKind(kind)
}

/**
 * 只看候选类别的图标映射。
 *
 * 这是引入声明形态之前的映射，保留为**兜底**：形态未定时图标退回旧样子，
 * 因此漏标形态只会少一次精细化，不会出现空图标。
 */
internal fun iconOfKind(kind: CompletionItemKind): Icon = when (kind) {
    CompletionItemKind.LOCAL -> AllIcons.Nodes.Variable
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

/** 候选是否出现在成员位置（用于在 Method 与 Function 图标之间选择）。 */
private fun CompletionItemKind.isMemberLike(): Boolean = when (this) {
    CompletionItemKind.MEMBER,
    CompletionItemKind.STATIC_MEMBER,
    CompletionItemKind.EXTEND_MEMBER,
    -> true

    else -> false
}
