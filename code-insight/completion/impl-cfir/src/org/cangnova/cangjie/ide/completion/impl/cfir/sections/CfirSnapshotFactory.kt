package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.asSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionSortDataImpl
import org.cangnova.cangjie.ide.completion.api.lookup.ImportCost
import org.cangnova.cangjie.ide.completion.api.lookup.ImportResolution
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
    importResolution: ImportResolution = ImportResolution.NoImport,
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
            // 身份键走与 extend 通道同源的 callableIdentityKey，不在这里另写一份公式。
            deduplicationKey = callableIdentityKey(symbol),
        )
        this.importPlan = importResolution
        this.sort = CompletionSortDataImpl(
            origin = origin,
            importCost = importCost,
            isDeprecated = symbol.isDeprecatedSymbol(),
            kind = kind,
        )
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
    importResolution: ImportResolution = ImportResolution.NoImport,
    constructor: CaConstructorSymbol? = null,
): CompletionSnapshot {
    val name = (classifier as? CaNamedSymbol)?.name?.asString().orEmpty()
    val handle = classifierIdentityKey(classifier)
    val qualifiedName = (classifier as? CaClassLikeSymbol)?.classId?.asSingleFqName()?.asString() ?: name
    // 构造器候选与类型候选的区别只有两点：尾文本渲染**实参组合**，插入时补一对括号。
    // 同一个类可以有多个构造器（不同实参组合），它们靠尾文本区分——
    // 只给一个 `Foo` 会让用户无从选择调用哪一种。
    val tailText = constructor?.let { renderConstructorTail(it) }.orEmpty()
    return completionSnapshot(
        lookupString = name.ifEmpty { handle },
        kind = kind,
        origin = origin,
        identityKind = CompletionIdentityKind.TYPE,
        qualifiedName = qualifiedName,
        tailText = tailText,
        typeText = name,
        documentStamp = documentStamp,
        insertParenthesis = constructor != null,
    ).apply {
        this.identity = CompletionIdentityData(
            kind = CompletionIdentityKind.TYPE,
            handle = handle,
            name = name,
            qualifiedName = qualifiedName,
            // 去重键必须含参数列表：同一个类的多个构造器共享 ClassId 身份，
            // 只用 `handle` 会把它们合并成一个候选。
            deduplicationKey = handle + tailText,
        )
        this.importPlan = importResolution
        this.sort = CompletionSortDataImpl(
            origin = origin,
            importCost = importCost,
            isDeprecated = classifier.isDeprecatedSymbol(),
            kind = kind,
        )
    }.build()
}

/**
 * 该符号是否带有内置弃用注解。
 *
 * 只认**内置** `@Deprecated`：自定义注解里恰好叫这个名字不是语言层面的弃用，
 * 把它们也算进来会让一批正常声明被降权。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaSymbol.isDeprecatedSymbol(): Boolean =
    (this as? CaAnnotatedSymbol)
        ?.annotations
        ?.any { it.builtInKind == BuiltInAnnotationKind.DEPRECATED } == true

/** 类状声明按 ClassId 区分来源；局部类型与类型参数没有全局 ClassId。 */
internal fun classifierIdentityKey(classifier: CaClassifierSymbol): String =
    (classifier as? CaClassLikeSymbol)?.classId?.toString()
        ?: (classifier as? CaNamedSymbol)?.name?.asString()
        ?: classifier.toString()

/**
 * 渲染构造器的实参组合。
 *
 * 构造函数是**特例**：`renderSignatureTail` 的 `(params): ReturnType` 形态对类型候选合适，
 * 而构造器候选要写的是一次调用，返回类型在调用位置上没有信息量，
 * 写上反而让 `Foo(a: Int64): Foo` 这种尾文本显得啰嗦。
 * 因此只渲染参数列表，并显示参数名（用户可能就是按名字选实参组合的）。
 */
@OptIn(CaExperimentalApi::class)
private fun renderConstructorTail(constructor: CaConstructorSymbol): String {
    val parameters = when (constructor) {
        is CaValueParameterOwnerSymbol -> constructor.valueParameters.map { renderParameter(it) }
        else -> emptyList()
    }
    return "(${parameters.joinToString(", ")})"
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
 * callable 的候选身份键。
 *
 * 与 [callableSnapshot] 写入 `deduplicationKey` 的值**必须同源**：
 * 区段用它判断「某个成员是否已由 extend 通道提供」，若两处算法不同，
 * 会把同一个成员同时按 MEMBER 和 EXTEND_MEMBER 各补一次。
 *
 * 键的构成是「展示名 + 渲染签名」而不是「展示名 + 展示尾文本」：
 * 尾文本带 `!`、`vararg`、`= ...` 等**调用形态**标记，那些是展示约定不是符号身份；
 * 用它当身份会让同一个声明因展示格式变化而被当成两个候选。
 * 参数类型与返回类型仍然进键，因此同名不同参数的重载天然不等价。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaSession.callableIdentityKey(symbol: CaCallableSymbol): String {
    val name = symbol.callableId?.toString() ?: symbol.displayName()
    val returnType = with(this) { symbol.asSignature() }.returnType
    val parameters = when (symbol) {
        is CaEnumConstructorSymbol -> symbol.payloadTypes.map { it.toString() }
        is CaValueParameterOwnerSymbol -> symbol.valueParameters.map { it.parameterIdentityPart() }
        else -> emptyList()
    }
    return "$name(${parameters.joinToString(", ")}): $returnType"
}

/**
 * 取形参参与身份比较的部分：类型与 vararg 形态，不含命名标记与默认值标记。
 */
private fun CaValueParameterSymbol.parameterIdentityPart(): String {
    val varargMark = if (isVararg) "vararg " else ""
    return "$varargMark${returnType}"
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