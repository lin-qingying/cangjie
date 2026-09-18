package org.cangnova.cangjie.analysis.api.cfir.components

import java.math.BigInteger
import org.cangnova.cangjie.analysis.api.annotations.*
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.impl.base.annotations.*
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.annotationInfo
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.source.psi

/** Analysis 只投影 CFIR 已发布的身份、绑定和常量，不解析源码文本或补造参数名。 */
internal fun CfirAnnotationCall.asPublicAnnotation(
    builder: CaSymbolByCfirBuilder,
    token: CaLifetimeToken,
): CaAnnotation = CaBaseAnnotationImpl(
    classId = annotationClassId,
    shortName = builtInDescriptor?.sourceName?.let(Name::identifier) ?: annotationClassId?.shortClassName,
    psi = source?.psi as? CjAnnotation,
    lazyArguments = lazy(LazyThreadSafetyMode.NONE) {
        argumentMapping.mapping.map { (name, value) ->
            CaBaseNamedAnnotationValue(name, value.asPublicAnnotationValue(token))
        }
    },
    constructorSymbol = resolveAnnotationConstructorSymbol(builder),
    token = token,
    builtInKind = annotationKind,
    isCompileTimeVisible = isCompileTimeVisible,
    isForcedCustom = forcedCustom.takeIf { isCompileTimeVisible != null },
    target = annotationTarget,
    runtimeVisible = runtimeVisible(builder),
    resolutionStatus = when (annotationResolveState) {
        CfirAnnotationResolveState.UNRESOLVED -> CaAnnotationResolutionStatus.UNKNOWN
        CfirAnnotationResolveState.TYPE_RESOLVED -> CaAnnotationResolutionStatus.TYPE_RESOLVED
        CfirAnnotationResolveState.ARGUMENTS_RESOLVED -> CaAnnotationResolutionStatus.ARGUMENTS_RESOLVED
        CfirAnnotationResolveState.SEMANTIC_RESOLVED -> CaAnnotationResolutionStatus.RESOLVED
        CfirAnnotationResolveState.ERROR -> CaAnnotationResolutionStatus.ERROR
    },
)

/**
 * 从注解类型声明的 ClassInfo 语义快照读取 runtime retention。
 *
 * 该字段属于注解类型而不是使用点；CJO 的官方 loader 同样只在
 * `AnnoKind_Annotation` 对应的 ClassInfo 上恢复它。找不到类型或元信息时保持未知，
 * 不把缺失的二进制字段当成 `false`。
 */
private fun CfirAnnotationCall.runtimeVisible(builder: CaSymbolByCfirBuilder): Boolean? {
    val classId = annotationClassId ?: return null
    val symbol = builder.analysisSession.cfirSession.symbolProvider.getClassLikeSymbolByClassId(classId)
        ?: return null
    symbol.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
    return symbol.cfir.annotationInfo?.runtimeVisible
}

/** 解析注解调用引用的构造器 symbol 并包装为公开的构造器 symbol；引用未解析时返回 `null`。 */
private fun CfirAnnotationCall.resolveAnnotationConstructorSymbol(builder: CaSymbolByCfirBuilder): CaConstructorSymbol? {
    val symbol = (calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol as? CfirConstructorSymbol ?: return null
    return builder.functionBuilder.buildConstructorSymbol(symbol)
}

/** 从注解类型的 cone 类型中提取 ClassId；类型未解析时返回 `null`。 */
internal fun CfirTypeRef.annotationClassIdOrNull(): ClassId? = coneTypeOrNull?.classIdOrPrimitiveClassId

/**
 * 把 CFIR 注解实参表达式转换为公开的注解值视图。
 *
 * 按表达式形态分发：常量值表达式、数组/tuple 字面量、枚举构造引用、
 * 内置 ABI 字面量各有专属转换；其余一律包装为 error value，
 * 不让未完成的常量求值伪装成合法结果。
 */
private fun CfirExpression.asPublicAnnotationValue(token: CaLifetimeToken): CaAnnotationValue {
    val psi = source?.psi as? CjElement
    return when (this) {
        is CfirConstantValueExpression -> constantValue.asPublicValue(psi, token)
        is CfirArrayLiteral -> CaBaseAnnotationValues.arrayValue(
            values = elements.map { it.asPublicAnnotationValue(token) },
            sourcePsi = psi,
            token = token,
        )
        is CfirTupleLiteral -> CaBaseAnnotationValues.tupleValue(
            values = elements.map { it.asPublicAnnotationValue(token) },
            sourcePsi = psi,
            token = token,
        )
        is CfirQualifiedAccessExpression -> {
            val constructor = (calleeReference as? CfirResolvedNamedReference)
                ?.resolvedSymbol as? CfirEnumConstructorSymbol
            if (constructor != null) {
                CaBaseAnnotationValues.enumValue(constructor.callableId, emptyList(), psi, token)
            } else {
                CaBaseAnnotationValues.constant(
                    CaBaseAnnotationValues.errorValue("Annotation reference is not a resolved enum value", psi),
                    psi,
                    token,
                )
            }
        }
        // 内置 ABI 字符串在 BODY_RESOLVE 已经验证，可在 BODY 常量规范化前查询。
        is CfirLiteralExpression -> {
            val constant = when {
                kind == CfirLiteralKind.STRING && value is String -> CaBaseAnnotationValues.stringValue(value as String, psi)
                kind == CfirLiteralKind.BOOLEAN && value is Boolean -> CaBaseAnnotationValues.boolValue(value as Boolean, psi)
                kind == CfirLiteralKind.UNIT -> CaBaseAnnotationValues.unitValue(psi)
                else -> CaBaseAnnotationValues.errorValue("Annotation constant evaluation has not completed", psi)
            }
            CaBaseAnnotationValues.constant(constant, psi, token)
        }
        else -> CaBaseAnnotationValues.constant(
            CaBaseAnnotationValues.errorValue("Annotation argument is not a resolved constant", psi), psi, token,
        )
    }
}

/** 对象字段来自 const 解释器的实际存储，不能把 ctor 调用实参冒充字段值。 */
private fun CfirConstantValue.asPublicValue(psi: CjElement?, token: CaLifetimeToken): CaAnnotationValue = when (this) {
    is CfirConstantValue.Primitive -> CaBaseAnnotationValues.constant(asPublicConstant(psi), psi, token)
    is CfirConstantValue.Tuple -> CaBaseAnnotationValues.tupleValue(elements.map { it.asPublicValue(psi, token) }, psi, token)
    is CfirConstantValue.ArrayValue -> CaBaseAnnotationValues.arrayValue(elements.map { it.asPublicValue(psi, token) }, psi, token)
    is CfirConstantValue.VArray -> CaBaseAnnotationValues.arrayValue(elements.map { it.asPublicValue(psi, token) }, psi, token)
    is CfirConstantValue.EnumValue -> CaBaseAnnotationValues.enumValue(constructor.callableId, arguments.map { it.asPublicValue(psi, token) }, psi, token)
    is CfirConstantValue.ObjectValue -> {
        val fields = fields.map { (symbol, value) -> CaBaseNamedAnnotationValue(symbol.name, value.asPublicValue(psi, token)) }
        if (type is ConeStructType) CaBaseAnnotationValues.structInstanceValue(type.classIdOrPrimitiveClassId, fields, psi, token)
        else CaBaseAnnotationValues.classInstanceValue(type.classIdOrPrimitiveClassId, fields, psi, token)
    }
}

/** 把 primitive 常量按其具体 primitive 种类转换为对应宽度的公开常量值；种类不明时返回 error value。 */
private fun CfirConstantValue.Primitive.asPublicConstant(psi: CjElement?): CaConstantValue = when (kind) {
    CfirLiteralKind.BOOLEAN -> CaBaseAnnotationValues.boolValue(value as Boolean, psi)
    CfirLiteralKind.RUNE -> CaBaseAnnotationValues.runeValue(value as Int, psi)
    CfirLiteralKind.STRING -> CaBaseAnnotationValues.stringValue(value as String, psi)
    CfirLiteralKind.UNIT -> CaBaseAnnotationValues.unitValue(psi)
    CfirLiteralKind.INT, CfirLiteralKind.BYTE -> {
        val integer = value as BigInteger
        when ((type as? ConePrimitiveType)?.kind) {
            PrimitiveTypeKind.INT8 -> CaBaseAnnotationValues.int8Value(integer.byteValueExact(), psi)
            PrimitiveTypeKind.INT16 -> CaBaseAnnotationValues.int16Value(integer.shortValueExact(), psi)
            PrimitiveTypeKind.INT32 -> CaBaseAnnotationValues.int32Value(integer.intValueExact(), psi)
            PrimitiveTypeKind.INT64, PrimitiveTypeKind.IDEAL_INT -> CaBaseAnnotationValues.int64Value(integer.longValueExact(), psi)
            PrimitiveTypeKind.INT_NATIVE -> CaBaseAnnotationValues.intNativeValue(integer.longValueExact(), psi)
            PrimitiveTypeKind.UINT8 -> CaBaseAnnotationValues.uint8Value(integer.toString().toUByte(), psi)
            PrimitiveTypeKind.UINT16 -> CaBaseAnnotationValues.uint16Value(integer.toString().toUShort(), psi)
            PrimitiveTypeKind.UINT32 -> CaBaseAnnotationValues.uint32Value(integer.toString().toUInt(), psi)
            PrimitiveTypeKind.UINT64 -> CaBaseAnnotationValues.uint64Value(integer.toString().toULong(), psi)
            PrimitiveTypeKind.UINT_NATIVE -> CaBaseAnnotationValues.uintNativeValue(integer.toString().toULong(), psi)
            else -> CaBaseAnnotationValues.errorValue("Unresolved integral constant type", psi)
        }
    }
    CfirLiteralKind.FLOAT -> when ((type as? ConePrimitiveType)?.kind) {
        PrimitiveTypeKind.FLOAT16 -> CaBaseAnnotationValues.float16Value((value as Double).toFloat(), psi)
        PrimitiveTypeKind.FLOAT32 -> CaBaseAnnotationValues.float32Value((value as Double).toFloat(), psi)
        PrimitiveTypeKind.FLOAT64, PrimitiveTypeKind.IDEAL_FLOAT -> CaBaseAnnotationValues.float64Value(value as Double, psi)
        else -> CaBaseAnnotationValues.errorValue("Unresolved floating constant type", psi)
    }
}
