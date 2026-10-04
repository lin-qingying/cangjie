package org.cangnova.cangjie.ide.completion.impl.shared

import org.cangnova.cangjie.ide.completion.api.CaCompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CaCompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CaCaretPolicy
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionIdentity
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionIdentityData
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionInsertionData
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionSortData
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionSortDataImpl
import org.cangnova.cangjie.ide.completion.api.lookup.CaImportResolution
import org.cangnova.cangjie.ide.completion.api.lookup.toWeight

/**
 * 补全快照的默认实现。
 *
 * 只保存不可变展示与插入数据：不含符号、类型、签名或分析会话，
 * 因此可以安全地在分析结束后继续被界面层持有并直接映射为平台 lookup 元素。
 */
data class CaCompletionSnapshotData(
    override val lookupString: String,
    override val kind: CaCompletionItemKind,
    override val identity: CaCompletionIdentity,
    override val sort: CaCompletionSortData,
    override val importPlan: CaImportResolution,
    override val insertion: CaCompletionInsertionData,
    override val tailText: String = "",
    override val typeText: String = "",
    override val weight: Int = sort.toWeight(),
) : CaCompletionSnapshot

/**
 * 快照构造器。
 *
 * 区段只需要给出「身份 + 来源 + 种类」，其余（权重、默认插入数据、默认导入结论）
 * 由构造器补齐。这样各区段不会各自拼一遍权重公式，
 * 也就不会出现「某个区段忘了算权重导致它被排到最后」这类难以察觉的偏差。
 */
class CaCompletionSnapshotBuilder(
    private val lookupString: String,
    private val kind: CaCompletionItemKind,
) {
    /** 候选身份。 */
    var identity: CaCompletionIdentity = CaCompletionIdentityData.of(
        kind = CaCompletionIdentityKind.KEYWORD,
        handle = lookupString,
        name = lookupString,
    )

    /** 结构化排序数据。 */
    var sort: CaCompletionSortData = CaCompletionSortDataImpl.of(CaCompletionOrigin.TOP_LEVEL)

    /** 导入结论。 */
    var importPlan: CaImportResolution = CaImportResolution.NoImport

    /** 插入数据。 */
    var insertion: CaCompletionInsertionData = CaCompletionInsertionData()

    /** 签名文本。 */
    var tailText: String = ""

    /** 渲染后的类型文本。 */
    var typeText: String = ""

    /** 构建不可变快照。 */
    fun build(): CaCompletionSnapshotData = CaCompletionSnapshotData(
        lookupString = lookupString,
        kind = kind,
        identity = identity,
        sort = sort,
        importPlan = importPlan,
        insertion = insertion,
        tailText = tailText,
        typeText = typeText,
    )
}

/**
 * 以显示名、种类与来源构造快照构造器。
 *
 * 身份句柄与去重键缺省取「显示名 + 签名」：仓颉允许同名不同参数的声明共存，
 * 只按显示名去重会直接吞掉重载候选，因此签名必须进入身份键。
 */
fun caCompletionSnapshot(
    lookupString: String,
    kind: CaCompletionItemKind,
    origin: CaCompletionOrigin,
    identityKind: CaCompletionIdentityKind = CaCompletionIdentityKind.SYMBOL,
    qualifiedName: String = "",
    tailText: String = "",
    typeText: String = "",
    documentStamp: Long = 0L,
    insertParenthesis: Boolean = false,
    namedArgumentPrefix: String = "",
): CaCompletionSnapshotBuilder = CaCompletionSnapshotBuilder(lookupString, kind).apply {
    this.identity = CaCompletionIdentityData(
        kind = identityKind,
        handle = if (tailText.isEmpty()) lookupString else "$lookupString$tailText",
        name = lookupString,
        qualifiedName = qualifiedName,
        deduplicationKey = if (tailText.isEmpty()) lookupString else "$lookupString$tailText",
    )
    this.sort = CaCompletionSortDataImpl.of(origin)
    this.tailText = tailText
    this.typeText = typeText
    this.insertion = CaCompletionInsertionData(
        insertParenthesis = insertParenthesis,
        namedArgumentPrefix = namedArgumentPrefix,
        // 有括号需求时 caret 必须落在括号内；否则落在插入文本之后。
        caretPolicy = if (insertParenthesis) CaCaretPolicy.INSIDE_NEW_PARENTHESIS else CaCaretPolicy.AFTER_INSERTED_TEXT,
        documentStamp = documentStamp,
    )
}

/**
 * 命名实参候选构造器。
 *
 * 仓颉命名实参的前缀是 `name: `。这里刻意不接受调用方传入分隔符：
 * 传错分隔符（尤其写成 Kotlin 的 `name = `）只能来自把 Kotlin 语法照搬的实现。
 */
fun caNamedArgumentSnapshot(
    parameterName: String,
    tailText: String = "",
    documentStamp: Long = 0L,
): CaCompletionSnapshotBuilder = caCompletionSnapshot(
    lookupString = parameterName,
    kind = CaCompletionItemKind.NAMED_ARGUMENT,
    origin = CaCompletionOrigin.NAMED_ARGUMENT,
    identityKind = CaCompletionIdentityKind.NAMED_ARGUMENT,
    tailText = tailText,
    namedArgumentPrefix = "$parameterName: ",
).apply {
    this.insertion = this.insertion.copy(
        caretPolicy = CaCaretPolicy.AFTER_NAMED_ARGUMENT_COLON,
        documentStamp = documentStamp,
    )
}

/**
 * 构建不可变快照。
 */
fun CaCompletionSnapshotBuilder.snapshot(): CaCompletionSnapshot = build()