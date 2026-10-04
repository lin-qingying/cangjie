package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.asSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionSortDataImpl
import org.cangnova.cangjie.ide.completion.api.lookup.ImportCost
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot

/**
 * callable 符号的展示名。
 *
 * 匿名或无名字符号回退到 `callableId`：给空串会让候选在弹窗里显示成空白行，
 * 比显示标识符更糟。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaCallableSymbol.displayName(): String =
    (this as? CaNamedSymbol)?.name?.asString() ?: callableId?.toString().orEmpty()

/**
 * callable 符号的限定名。
 *
 * 用于界面期复查与导入计划，不作为弹窗主文本展示。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaCallableSymbol.qualifiedNameText(): String =
    callableId?.asSingleFqName()?.asString() ?: displayName()

/**
 * 把 callable 符号转换为稳定的补全快照。
 *
 * **必须在 Analysis 会话内调用**：签名渲染依赖会话组件，
 * 快照离开会话后不再持有符号或签名。
 *
 * 渲染完整参数列表而非只给返回类型：仓颉允许同名不同参数的声明共存，
 * 签名同时承担去重区分职责，不渲染会把合法重载合并成一个候选。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaSession.callableSnapshot(
    symbol: CaCallableSymbol,
    kind: CompletionItemKind,
    origin: CompletionOrigin,
    importCost: Int = ImportCost.DIRECT,
    documentStamp: Long = 0L,
): CompletionSnapshot {
    val name = symbol.displayName()
    val signature = with(this) { symbol.asSignature() }
    val tailText = renderSignatureTail(symbol, signature.returnType)
    return completionSnapshot(
        lookupString = name,
        kind = kind,
        origin = origin,
        identityKind = CompletionIdentityKind.SYMBOL,
        tailText = tailText,
        typeText = signature.returnType.toString(),
        documentStamp = documentStamp,
        insertParenthesis = true,
    ).apply {
        // 身份句柄 = 稳定调用标识 + 渲染签名。
        //
        // 只用短名无法重新解析到具体声明；只用 callableId 又对同名重载给出同一个值，
        // 恢复时只能落到任意一个重载。两者拼接才同时满足「可定位」与「可区分」。
        val qualified = symbol.qualifiedNameText()
        this.identity = CompletionIdentityData(
            kind = CompletionIdentityKind.SYMBOL,
            handle = "$qualified$tailText",
            name = name,
            qualifiedName = qualified,
            deduplicationKey = if (tailText.isEmpty()) name else "$name$tailText",
        )
        this.sort = CompletionSortDataImpl(origin = origin, importCost = importCost)
    }.build()
}

/**
 * 把分类器符号转换为稳定的补全快照。
 *
 * 类型候选不带括号：类型后面跟的是类型实参列表而不是调用实参列表，
 * 跟一个左括号会让用户写出 `List(` 这样的非法源码。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaSession.classifierSnapshot(
    classifier: CaClassifierSymbol,
    kind: CompletionItemKind,
    origin: CompletionOrigin,
    importCost: Int = ImportCost.DIRECT,
    documentStamp: Long = 0L,
): CompletionSnapshot {
    val name = (classifier as? CaNamedSymbol)?.name?.asString().orEmpty()
    val handle = name.ifEmpty { classifier.toString() }
    return completionSnapshot(
        lookupString = handle,
        kind = kind,
        origin = origin,
        identityKind = CompletionIdentityKind.TYPE,
        qualifiedName = name,
        documentStamp = documentStamp,
    ).apply {
        this.identity = CompletionIdentityData(
            kind = CompletionIdentityKind.TYPE,
            handle = handle,
            name = handle,
            qualifiedName = name,
            deduplicationKey = handle,
        )
        this.sort = CompletionSortDataImpl(origin = origin, importCost = importCost)
    }.build()
}

/**
 * 渲染签名尾文本。
 *
 * 显式展开参数列表而不是直接用各符号的 `toString()`：三个 callable 家族的
 * `toString()` 形态并不一致，混用会让同类候选的展示格式参差。
 *
 * 参数标注遵循仓颉源码形态：
 * - `!` 表示该形参在调用侧是命名实参；
 * - `vararg` 前缀表示可变长；
 * - `= ...` 表示声明了默认值。
 */
private fun renderSignatureTail(symbol: CaCallableSymbol, returnType: CaType): String {
    val parameters = when (symbol) {
        is CaEnumConstructorSymbol -> symbol.payloadTypes.map { it.toString() }
        is CaValueParameterOwnerSymbol -> symbol.valueParameters.map { renderParameter(it) }
        else -> emptyList()
    }
    return "(${parameters.joinToString(", ")}): $returnType"
}

/**
 * 渲染单个形参。
 */
@OptIn(CaExperimentalApi::class)
private fun renderParameter(parameter: CaValueParameterSymbol): String {
    val name = (parameter as? CaNamedSymbol)?.name?.asString() ?: "_"
    val type = parameter.returnType.toString()
    val namedMark = if (parameter.isNamed) "!" else ""
    val varargMark = if (parameter.isVararg) "vararg " else ""
    val defaultMark = if (parameter.hasDefaultValue) " = ..." else ""
    return "$varargMark$name$namedMark: $type$defaultMark"
}

/**
 * callable 的候选去重键。
 *
 * 与 [callableSnapshot] 内部写入身份的键**必须同源**：
 * 区段用它判断「某个成员是否已由 extend 通道提供」，若两处算法不同，
 * 会把同一个成员同时按 MEMBER 和 EXTEND_MEMBER 各补一次。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaSession.callableIdentityKey(symbol: CaCallableSymbol): String {
    val name = symbol.displayName()
    val tailText = renderSignatureTail(symbol, with(this) { symbol.asSignature() }.returnType)
    return if (tailText.isEmpty()) name else "$name$tailText"
}

/**
 * 按候选身份去重。
 *
 * 只合并**确实同一**的候选：同名但签名不同者身份键不同，必须各自保留。
 */
internal fun List<CompletionSnapshot>.distinctByIdentity(): List<CompletionSnapshot> {
    val seen = mutableSetOf<String>()
    return filter { seen.add(it.identity.deduplicationKey) }
}

/**
 * 取得函数族符号的形参列表，供命名实参候选（9.4）复用。
 *
 * 非函数族返回空列表：枚举构造器的 payload 是位置实参，没有命名实参形态。
 */
internal fun CaCallableSymbol.namedParameterList(): List<CaValueParameterSymbol> =
    (this as? CaValueParameterOwnerSymbol)?.valueParameters.orEmpty().filter { it.isNamed }