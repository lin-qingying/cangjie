package org.cangnova.cangjie.analysis.api.cfir.evaluate

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue
import org.cangnova.cangjie.analysis.api.annotations.CaNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.evaluateCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaCollectionCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaScalarCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaTupleCompileTimeValue
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseAnnotationValues
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.resolvedType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjCollectionLiteralExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjTupleExpression
import org.cangnova.cangjie.psi.CjValueArgument

/**
 * CFIR 注解参数值转换器。
 *
 * 对齐 Kotlin `FirAnnotationValueConverter` 的职责边界：
 * 注解对象构建仍留在 annotations 入口层，
 * 而表达式到公开注解值模型的转换统一收敛在 evaluate 层。
 */
internal fun CjValueArgument.asPublicNamedAnnotationValue(
    session: CaCfirSession,
    token: CaLifetimeToken,
    fallbackName: Name?,
    position: Int,
): CaNamedAnnotationValue {
    val argumentName = getArgumentName()?.asName
        ?: fallbackName
        ?: if (position == 0) StandardNames.DEFAULT_VALUE_PARAMETER else Name.special("<annotation-arg-$position>")
    val expression = getArgumentExpression()
    return CaBaseNamedAnnotationValue(
        name = argumentName,
        expression = expression?.asPublicAnnotationValue(session, token)
            ?: CaBaseAnnotationValues.constant(
                value = CaBaseAnnotationValues.errorValue(
                    errorMessage = "Annotation argument expression is missing",
                    sourcePsi = asElement(),
                ),
                sourcePsi = asElement(),
                token = token,
            ),
    )
}

/**
 * 将源码级注解参数表达式映射到公开注解值模型。
 */
internal fun CjExpression.asPublicAnnotationValue(
    session: CaCfirSession,
    token: CaLifetimeToken,
): CaAnnotationValue {
    when (this) {
        is CjTupleExpression -> {
            return CaBaseAnnotationValues.tupleValue(
                values = expressions.map { element -> element.asPublicAnnotationValue(session, token) },
                sourcePsi = this,
                token = token,
            )
        }

        is CjCollectionLiteralExpression -> {
            return CaBaseAnnotationValues.tupleValue(
                values = innerExpressions.map { element -> element.asPublicAnnotationValue(session, token) },
                sourcePsi = this,
                token = token,
            )
        }

        is CjCallExpression -> {
            resolveCallLikeAnnotationValue(session, token)?.let { return it }
        }
    }

    session.evaluateCompileTimeValueAsAnnotationValue(this, token)?.let { annotationValue ->
        return annotationValue
    }

    return CaBaseAnnotationValues.constant(
        value = CaBaseAnnotationValues.errorValue(
            errorMessage = "Unsupported annotation argument expression `${text}`",
            sourcePsi = this,
        ),
        sourcePsi = this,
        token = token,
    )
}

private fun CjCallExpression.resolveCallLikeAnnotationValue(
    session: CaCfirSession,
    token: CaLifetimeToken,
): CaAnnotationValue? {
    val resolvedSymbol = with(session) { resolveToSymbol() }
    return when (resolvedSymbol) {
        is CaEnumConstructorSymbol -> {
            CaBaseAnnotationValues.enumValue(
                callableId = resolvedSymbol.callableId,
                arguments = valueArguments.map { argument ->
                    (argument as CjValueArgument).getArgumentExpression()
                        ?.asPublicAnnotationValue(session, token)
                        ?: CaBaseAnnotationValues.constant(
                            value = CaBaseAnnotationValues.errorValue(
                                errorMessage = "Enum annotation argument expression is missing",
                                sourcePsi = argument.asElement(),
                            ),
                            sourcePsi = argument.asElement(),
                            token = token,
                        )
                },
                sourcePsi = this,
                token = token,
            )
        }

        is CaConstructorSymbol -> {
            val owner = resolvedSymbol.containingDeclaration as? CaClassSymbol
            val namedArguments = valueArguments.mapIndexed { index, argument ->
                (argument as CjValueArgument).asPublicNamedAnnotationValue(
                    session = session,
                    token = token,
                    fallbackName = resolvedSymbol.valueParameters.getOrNull(index)?.name,
                    position = index,
                )
            }
            when (owner?.classKind) {
                CaClassKind.STRUCT -> CaBaseAnnotationValues.structInstanceValue(
                    classId = owner.classId,
                    arguments = namedArguments,
                    sourcePsi = this,
                    token = token,
                )

                else -> CaBaseAnnotationValues.classInstanceValue(
                    classId = owner?.classId,
                    arguments = namedArguments,
                    sourcePsi = this,
                    token = token,
                )
            }
        }

        else -> null
    }
}

/**
 * 将编译期常量结果投影到公开注解值模型。
 */
private fun CaCompileTimeValue.asPublicAnnotationValue(
    sourcePsi: CjExpression,
    session: CaCfirSession,
    token: CaLifetimeToken,
): CaAnnotationValue {
    return when (this) {
        is CaScalarCompileTimeValue -> CaBaseAnnotationValues.constant(
            value = toPublicConstantValue(sourcePsi, session),
            sourcePsi = sourcePsi,
            token = token,
        )

        is CaTupleCompileTimeValue -> CaBaseAnnotationValues.tupleValue(
            values = elements.map { element -> element.asPublicAnnotationValue(sourcePsi, session, token) },
            sourcePsi = sourcePsi,
            token = token,
        )

        is CaCollectionCompileTimeValue -> CaBaseAnnotationValues.tupleValue(
            values = elements.map { element -> element.asPublicAnnotationValue(sourcePsi, session, token) },
            sourcePsi = sourcePsi,
            token = token,
        )
    }
}

private fun CaCfirSession.evaluateCompileTimeValueAsAnnotationValue(
    expression: CjExpression,
    token: CaLifetimeToken,
): CaAnnotationValue? =
    evaluateCompileTimeValue(expression)?.asPublicAnnotationValue(expression, this, token)

/**
 * 依据求值后表达式的真实 `ClassId` 恢复公开常量类型。
 *
 * 这里不把 `Int8/UInt16/Float32` 退化成字符串常量，而是恢复到明确的仓颉标量类型。
 */
private fun CaScalarCompileTimeValue.toPublicConstantValue(
    sourcePsi: CjExpression,
    session: CaCfirSession,
): org.cangnova.cangjie.analysis.api.annotations.CaConstantValue {
    val typeClassId = sourcePsi.sessionExpressionClassId(session)
    return when (typeClassId) {
        BOOL_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.boolValue(renderedText == "true", sourcePsi)
        }

        RUNE_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.runeValue(parseRuneLiteral(renderedText), sourcePsi)
        }

        INT8_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.int8Value(renderedText.removeNumericSuffix().toByte(), sourcePsi)
        }

        INT16_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.int16Value(renderedText.removeNumericSuffix().toShort(), sourcePsi)
        }

        INT32_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.int32Value(renderedText.removeNumericSuffix().toInt(), sourcePsi)
        }

        INT64_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.int64Value(renderedText.removeNumericSuffix().toLong(), sourcePsi)
        }

        INT_NATIVE_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.intNativeValue(renderedText.removeNumericSuffix().toLong(), sourcePsi)
        }

        UINT8_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.uint8Value(renderedText.removeNumericSuffix().toUByte(), sourcePsi)
        }

        UINT16_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.uint16Value(renderedText.removeNumericSuffix().toUShort(), sourcePsi)
        }

        UINT32_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.uint32Value(renderedText.removeNumericSuffix().toUInt(), sourcePsi)
        }

        UINT64_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.uint64Value(renderedText.removeNumericSuffix().toULong(), sourcePsi)
        }

        UINT_NATIVE_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.uintNativeValue(renderedText.removeNumericSuffix().toULong(), sourcePsi)
        }

        FLOAT16_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.float16Value(renderedText.removeNumericSuffix().toFloat(), sourcePsi)
        }

        FLOAT32_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.float32Value(renderedText.removeNumericSuffix().toFloat(), sourcePsi)
        }

        FLOAT64_CLASS_ID -> scalarConstantOrError(sourcePsi) {
            CaBaseAnnotationValues.float64Value(renderedText.removeNumericSuffix().toDouble(), sourcePsi)
        }

        STRING_CLASS_ID -> CaBaseAnnotationValues.stringValue(renderedText, sourcePsi)

        else -> when (kind) {
            org.cangnova.cangjie.analysis.api.evaluation.CaScalarValueKind.BOOLEAN ->
                CaBaseAnnotationValues.boolValue(renderedText == "true", sourcePsi)

            org.cangnova.cangjie.analysis.api.evaluation.CaScalarValueKind.RUNE ->
                scalarConstantOrError(sourcePsi) {
                    CaBaseAnnotationValues.runeValue(parseRuneLiteral(renderedText), sourcePsi)
                }

            org.cangnova.cangjie.analysis.api.evaluation.CaScalarValueKind.STRING ->
                CaBaseAnnotationValues.stringValue(renderedText, sourcePsi)

            org.cangnova.cangjie.analysis.api.evaluation.CaScalarValueKind.INTEGER ->
                CaBaseAnnotationValues.errorValue(
                    errorMessage = "Cannot determine the exact integer annotation value type for `$renderedText`",
                    sourcePsi = sourcePsi,
                )

            org.cangnova.cangjie.analysis.api.evaluation.CaScalarValueKind.FLOAT ->
                CaBaseAnnotationValues.errorValue(
                    errorMessage = "Cannot determine the exact floating annotation value type for `$renderedText`",
                    sourcePsi = sourcePsi,
                )

            else -> CaBaseAnnotationValues.errorValue(
                errorMessage = "Unsupported scalar annotation value `$renderedText`",
                sourcePsi = sourcePsi,
            )
        }
    }
}

private fun CjExpression.sessionExpressionClassId(session: CaCfirSession): ClassId? =
    (getOrBuildCfir(session.resolutionFacade) as? CfirExpression)?.resolvedType?.classIdOrPrimitiveClassId

/**
 * 将“文本到类型化常量”的解析失败收束为结构化错误常量。
 */
private inline fun scalarConstantOrError(
    sourcePsi: CjExpression,
    builder: () -> org.cangnova.cangjie.analysis.api.annotations.CaConstantValue,
): org.cangnova.cangjie.analysis.api.annotations.CaConstantValue {
    return runCatching(builder).getOrElse { throwable ->
        CaBaseAnnotationValues.errorValue(
            errorMessage = throwable.message ?: "Failed to parse annotation scalar constant",
            sourcePsi = sourcePsi,
        )
    }
}

private val BOOL_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.boolFqName)
private val RUNE_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.runeFqName)
private val STRING_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.stringFqName)
private val INT8_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.int8FqName)
private val INT16_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.int16FqName)
private val INT32_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.int32FqName)
private val INT64_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.int64FqName)
private val INT_NATIVE_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.int_nativeFqName)
private val UINT8_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.uint8FqName)
private val UINT16_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.uint16FqName)
private val UINT32_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.uint32FqName)
private val UINT64_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.uint64FqName)
private val UINT_NATIVE_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.uint_nativeFqName)
private val FLOAT16_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.float16FqName)
private val FLOAT32_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.float32FqName)
private val FLOAT64_CLASS_ID: ClassId = ClassId.topLevel(StandardNames.FqNames.float64FqName)

private fun String.removeNumericSuffix(): String =
    trim().removeSuffix("i8").removeSuffix("i16").removeSuffix("i32").removeSuffix("i64")
        .removeSuffix("inative")
        .removeSuffix("u8").removeSuffix("u16").removeSuffix("u32").removeSuffix("u64")
        .removeSuffix("unative")
        .removeSuffix("f16").removeSuffix("f32").removeSuffix("f64")

private fun parseRuneLiteral(text: String): Int {
    val body = text.removePrefix("'").removeSuffix("'")
    return when {
        body.startsWith("\\u") && body.length >= 6 -> body.removePrefix("\\u").take(4).toInt(16)
        body.startsWith("\\U") && body.length >= 10 -> body.removePrefix("\\U").take(8).toInt(16)
        body.startsWith("\\") && body.length == 2 -> body[1].code
        body.isNotEmpty() -> body.first().code
        else -> 0
    }
}
