package org.cangnova.cangjie.ide.completion.impl.shared

import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CaretPolicy
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentity
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionInsertionData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionSortData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionSortDataImpl
import org.cangnova.cangjie.ide.completion.api.lookup.ImportResolution
import org.cangnova.cangjie.ide.completion.api.lookup.toWeight

/**
 * 补全快照的默认实现。
 *
 * 只保存不可变展示与插入数据：不含符号、类型、签名或分析会话，
 * 因此可以安全地在分析结束后继续被界面层持有并直接映射为平台 lookup 元素。
 */
data class CompletionSnapshotData(
    override val lookupString: String,
    override val kind: CompletionItemKind,
    override val identity: CompletionIdentity,
    override val sort: CompletionSortData,
    override val importPlan: ImportResolution,
    override val insertion: CompletionInsertionData,
    override val tailText: String = "",
    override val typeText: String = "",
    override val weight: Int = sort.toWeight(),
) : CompletionSnapshot

/**
 * 快照构造器。
 *
 * 区段只需要给出「身份 + 来源 + 种类」，其余（权重、默认插入数据、默认导入结论）
 * 由构造器补齐。这样各区段不会各自拼一遍权重公式，
 * 也就不会出现「某个区段忘了算权重导致它被排到最后」这类难以察觉的偏差。
 */
class CompletionSnapshotBuilder(
    private val lookupString: String,
    private val kind: CompletionItemKind,
) {
    /** 候选身份。 */
    var identity: CompletionIdentity = CompletionIdentityData.of(
        kind = CompletionIdentityKind.KEYWORD,
        handle = lookupString,
        name = lookupString,
    )

    /** 结构化排序数据。 */
    var sort: CompletionSortData = CompletionSortDataImpl.of(CompletionOrigin.TOP_LEVEL)

    /** 导入结论。 */
    var importPlan: ImportResolution = ImportResolution.NoImport

    /** 插入数据。 */
    var insertion: CompletionInsertionData = CompletionInsertionData()

    /** 签名文本。 */
    var tailText: String = ""

    /** 渲染后的类型文本。 */
    var typeText: String = ""

    /** 构建不可变快照。 */
    fun build(): CompletionSnapshotData = CompletionSnapshotData(
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
fun completionSnapshot(
    lookupString: String,
    kind: CompletionItemKind,
    origin: CompletionOrigin,
    identityKind: CompletionIdentityKind = CompletionIdentityKind.SYMBOL,
    qualifiedName: String = "",
    tailText: String = "",
    typeText: String = "",
    documentStamp: Long = 0L,
    insertParenthesis: Boolean = false,
    namedArgumentPrefix: String = "",
): CompletionSnapshotBuilder = CompletionSnapshotBuilder(lookupString, kind).apply {
    this.identity = CompletionIdentityData(
        kind = identityKind,
        handle = if (tailText.isEmpty()) lookupString else "$lookupString$tailText",
        name = lookupString,
        qualifiedName = qualifiedName,
        deduplicationKey = if (tailText.isEmpty()) lookupString else "$lookupString$tailText",
    )
    this.sort = CompletionSortDataImpl.of(origin)
    this.tailText = tailText
    this.typeText = typeText
    this.insertion = CompletionInsertionData(
        insertParenthesis = insertParenthesis,
        namedArgumentPrefix = namedArgumentPrefix,
        // 有括号需求时 caret 必须落在括号内；否则落在插入文本之后。
        caretPolicy = if (insertParenthesis) CaretPolicy.INSIDE_NEW_PARENTHESIS else CaretPolicy.AFTER_INSERTED_TEXT,
        documentStamp = documentStamp,
    )
}

/**
 * 命名实参候选构造器。
 *
 * 仓颉命名实参的前缀是 `name: `。这里刻意不接受调用方传入分隔符：
 * 传错分隔符（尤其写成 Kotlin 的 `name = `）只能来自把 Kotlin 语法照搬的实现。
 */
fun namedArgumentSnapshot(
    parameterName: String,
    tailText: String = "",
    documentStamp: Long = 0L,
): CompletionSnapshotBuilder = completionSnapshot(
    lookupString = parameterName,
    kind = CompletionItemKind.NAMED_ARGUMENT,
    origin = CompletionOrigin.NAMED_ARGUMENT,
    identityKind = CompletionIdentityKind.NAMED_ARGUMENT,
    tailText = tailText,
    namedArgumentPrefix = "$parameterName: ",
).apply {
    this.insertion = this.insertion.copy(
        caretPolicy = CaretPolicy.AFTER_NAMED_ARGUMENT_COLON,
        documentStamp = documentStamp,
    )
}

/**
 * 构建不可变快照。
 */
fun CompletionSnapshotBuilder.snapshot(): CompletionSnapshot = build()