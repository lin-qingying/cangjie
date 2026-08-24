package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue
import org.cangnova.cangjie.analysis.api.annotations.CaNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.evaluate.asPublicNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseAnnotationImpl
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseAnnotationValues
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjValueArgument


/**
 * 从源码 PSI 注解构造公开 Analysis API 注解视图。
 */
internal fun CjAnnotation.asPublicAnnotation(
    session: CaCfirSession,
    token: CaLifetimeToken,
): CaAnnotation {
    val constructorSymbol = resolveAnnotationConstructorSymbol(session)
    return CaBaseAnnotationImpl(
        classId = resolveAnnotationClassId(session),
        shortName = shortName,
        psi = this,
        lazyArguments = lazy(LazyThreadSafetyMode.NONE) {
            buildPublicNamedArguments(
                session = session,
                token = token,
                constructorSymbol = constructorSymbol,
            )
        },
        constructorSymbol = constructorSymbol,
        token = token,
    )
}

/**
 * 从 CFIR 注解调用直接构造 Analysis API 注解视图。
 *
 * 宏展开后的 annotation 可能没有 PSI，本入口必须只依赖 CFIR 的 typeRef、
 * calleeReference 与 argumentList，避免 public annotations 重新落回 PSI。
 */
internal fun CfirAnnotationCall.asPublicAnnotation(
    builder: CaSymbolByCfirBuilder,
    token: CaLifetimeToken,
): CaAnnotation {
    val constructorSymbol = resolveAnnotationConstructorSymbol(builder)
    val classId = typeRef.annotationClassIdOrNull()
    return CaBaseAnnotationImpl(
        classId = classId,
        shortName = classId?.shortClassName ?: (calleeReference as? CfirNamedReference)?.name ?: source.annotationShortNameOrNull(),
        psi = source?.psi as? CjAnnotation,
        lazyArguments = lazy(LazyThreadSafetyMode.NONE) {
            argumentList.asPublicNamedAnnotationValues(token)
        },
        constructorSymbol = constructorSymbol,
        token = token,
    )
}

/**
 * 从注解调用点恢复其目标 class-like 标识。
 */
private fun CjAnnotation.resolveAnnotationClassId(session: CaCfirSession): ClassId? {
    val constructorReference = calleeExpression?.constructorReferenceExpression ?: return null
    val targetSymbol = with(session) { constructorReference.resolveToSymbol() }
    return (targetSymbol as? CaClassLikeSymbol)?.classId
}

/**
 * 恢复注解调用解析到的构造器符号。
 */
private fun CjAnnotation.resolveAnnotationConstructorSymbol(session: CaCfirSession): CaConstructorSymbol? {
    val constructorReference = calleeExpression?.constructorReferenceExpression ?: return null
    return with(session) { constructorReference.resolveToSymbol() as? CaConstructorSymbol }
}

/**
 * 构建注解“命名参数 + 值对象”列表。
 */
private fun CjAnnotation.buildPublicNamedArguments(
    session: CaCfirSession,
    token: CaLifetimeToken,
    constructorSymbol: CaConstructorSymbol?,
): List<CaNamedAnnotationValue> {
    return valueArguments.mapIndexed { index, argument ->
        (argument as CjValueArgument).asPublicNamedAnnotationValue(
            session = session,
            token = token,
            fallbackName = constructorSymbol?.valueParameters?.getOrNull(index)?.name,
            position = index,
        )
    }
}

/**
 * 从 CFIR 注解调用引用中恢复构造器公开符号。
 */
private fun CfirAnnotationCall.resolveAnnotationConstructorSymbol(builder: CaSymbolByCfirBuilder): CaConstructorSymbol? {
    val symbol = when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        else -> null
    } as? CfirConstructorSymbol ?: return null
    return builder.functionBuilder.buildConstructorSymbol(symbol)
}

/**
 * 从已解析注解类型引用中提取注解类 classId。
 */
internal fun CfirTypeRef.annotationClassIdOrNull(): ClassId? =
    (this as? CfirResolvedTypeRef)?.coneType?.classIdOrPrimitiveClassId

/**
 * 将 CFIR 注解参数列表转换为公开命名注解参数列表。
 */
private fun CfirArgumentList.asPublicNamedAnnotationValues(token: CaLifetimeToken): List<CaNamedAnnotationValue> {
    val resolvedMapping = (this as? CfirResolvedArgumentList)?.mapping
    if (resolvedMapping != null && resolvedMapping.isNotEmpty()) {
        return resolvedMapping.entries.mapIndexed { index, (argument, parameter) ->
            argument.asPublicNamedAnnotationValue(
                name = parameter.name,
                position = index,
                token = token,
            )
        }
    }

    return arguments.mapIndexed { index, argument ->
        argument.asPublicNamedAnnotationValue(
            name = if (index == 0) StandardNames.DEFAULT_VALUE_PARAMETER else Name.special("<annotation-arg-$index>"),
            position = index,
            token = token,
        )
    }
}

/**
 * 将单个 CFIR 注解实参转换为公开命名注解值。
 */
private fun CfirExpression.asPublicNamedAnnotationValue(
    name: Name,
    position: Int,
    token: CaLifetimeToken,
): CaNamedAnnotationValue {
    return CaBaseNamedAnnotationValue(
        name = name,
        expression = asPublicAnnotationValue(
            token = token,
            errorMessage = "Unsupported CFIR annotation argument at position $position",
        ),
    )
}

/**
 * 将 CFIR 注解实参表达式转换为公开注解值对象。
 */
private fun CfirExpression.asPublicAnnotationValue(
    token: CaLifetimeToken,
    errorMessage: String = "Unsupported CFIR annotation argument `${source?.text}`",
): CaAnnotationValue {
    val sourcePsi = source?.psi as? CjElement
    return when (this) {
        is CfirLiteralExpression -> literalAsPublicAnnotationValue(sourcePsi, token)
        is CfirTupleLiteral -> CaBaseAnnotationValues.tupleValue(
            values = elements.map { element -> element.asPublicAnnotationValue(token) },
            sourcePsi = sourcePsi,
            token = token,
        )
        is CfirArrayLiteral -> CaBaseAnnotationValues.tupleValue(
            values = elements.map { element -> element.asPublicAnnotationValue(token) },
            sourcePsi = sourcePsi,
            token = token,
        )
        else -> CaBaseAnnotationValues.constant(
            value = CaBaseAnnotationValues.errorValue(errorMessage, sourcePsi),
            sourcePsi = sourcePsi,
            token = token,
        )
    }
}

/**
 * 将 CFIR 字面量表达式转换为公开常量注解值。
 */
private fun CfirLiteralExpression.literalAsPublicAnnotationValue(
    sourcePsi: CjElement?,
    token: CaLifetimeToken,
): CaAnnotationValue {
    val constantValue = when (kind) {
        CfirLiteralKind.BOOLEAN -> CaBaseAnnotationValues.boolValue(value == true, sourcePsi)
        CfirLiteralKind.RUNE -> CaBaseAnnotationValues.runeValue((value as? Number)?.toInt() ?: source?.text?.toString().orEmpty().parseRuneLiteral(), sourcePsi)
        CfirLiteralKind.STRING -> CaBaseAnnotationValues.stringValue(value?.toString().orEmpty(), sourcePsi)
        CfirLiteralKind.INT -> CaBaseAnnotationValues.int64Value((value as? Number)?.toLong() ?: source?.text?.toString().orEmpty().removeNumericSuffix().toLongOrNull() ?: 0L, sourcePsi)
        CfirLiteralKind.BYTE -> CaBaseAnnotationValues.int64Value((value as? Number)?.toLong() ?: source?.text?.toString().orEmpty().removeNumericSuffix().toLongOrNull() ?: 0L, sourcePsi)
        CfirLiteralKind.FLOAT -> CaBaseAnnotationValues.float64Value((value as? Number)?.toDouble() ?: source?.text?.toString().orEmpty().removeNumericSuffix().toDoubleOrNull() ?: 0.0, sourcePsi)
        CfirLiteralKind.UNIT -> CaBaseAnnotationValues.errorValue("Unit is not a valid annotation constant", sourcePsi)
    }
    return CaBaseAnnotationValues.constant(
        value = constantValue,
        sourcePsi = sourcePsi,
        token = token,
    )
}

/**
 * 在缺少 PSI 时从 source 文本兜出注解短名。
 */
private fun org.cangnova.cangjie.source.CjSourceElement?.annotationShortNameOrNull(): Name? {
    val rawText = this?.text?.toString()?.trim().orEmpty()
    if (!rawText.startsWith("@")) return null
    val shortName = rawText
        .removePrefix("@!")
        .removePrefix("@")
        .substringBefore('[')
        .substringBefore('(')
        .substringAfterLast('.')
        .trim()
    return Name.identifierIfValid(shortName)
}

/**
 * 去除仓颉数字字面量的类型后缀，便于按基础数值解析。
 */
private fun String.removeNumericSuffix(): String =
    trim().removeSuffix("i8").removeSuffix("i16").removeSuffix("i32").removeSuffix("i64")
        .removeSuffix("inative")
        .removeSuffix("u8").removeSuffix("u16").removeSuffix("u32").removeSuffix("u64")
        .removeSuffix("unative")
        .removeSuffix("f16").removeSuffix("f32").removeSuffix("f64")

/**
 * 解析 rune 字面量文本为公开注解常量使用的码点。
 */
private fun String.parseRuneLiteral(): Int {
    val body = trim().removePrefix("'").removeSuffix("'")
    return when {
        body.startsWith("\\u") && body.length >= 6 -> body.removePrefix("\\u").take(4).toIntOrNull(16) ?: 0
        body.startsWith("\\U") && body.length >= 10 -> body.removePrefix("\\U").take(8).toIntOrNull(16) ?: 0
        body.startsWith("\\") && body.length == 2 -> body[1].code
        body.isNotEmpty() -> body.first().code
        else -> 0
    }
}
