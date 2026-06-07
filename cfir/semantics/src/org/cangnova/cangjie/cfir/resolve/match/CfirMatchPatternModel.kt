package org.cangnova.cangjie.cfir.resolve.match

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.payloadArity
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.ClassId

typealias CfirMatrix = List<List<CfirMatchPattern>>

private const val OPTION_SOME_CONSTRUCTOR_NAME = "Some"
private const val OPTION_NONE_CONSTRUCTOR_NAME = "None"

data class CfirMatchPattern(
    val type: ConeCangJieType,
    val kind: CfirMatchPatternKind,
    val cfirPattern: CfirPattern? = null,
) {
    fun text(): String = when (kind) {
        CfirMatchPatternKind.Wild -> "_"
        CfirMatchPatternKind.Error -> "<error>"
        is CfirMatchPatternKind.Binding -> kind.name
        is CfirMatchPatternKind.Type -> kind.name ?: kind.type.toString()
        is CfirMatchPatternKind.Const -> kind.value.toText()
        is CfirMatchPatternKind.Tuple -> kind.subPatterns.joinToString(", ", "(", ")") { it.text() }
        is CfirMatchPatternKind.Enum -> {
            if (kind.subPatterns.isEmpty()) kind.entryName
            else "${kind.entryName}(${kind.subPatterns.joinToString(", ") { it.text() }})"
        }
    }

    val constructors: List<CfirConstructor>
        get() = when (val k = kind) {
            CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding, CfirMatchPatternKind.Error -> emptyList()
            is CfirMatchPatternKind.Const -> listOf(CfirConstructor.ConstantValue(k.value))
            is CfirMatchPatternKind.Tuple -> listOf(CfirConstructor.Single)
            is CfirMatchPatternKind.Type -> listOf(CfirConstructor.Type(k.type))
            is CfirMatchPatternKind.Enum -> listOf(
                CfirConstructor.Enum(k.enumClassId, k.entryName, k.subPatterns.size)
            )
        }

    val ergonomicType: ConeCangJieType
        get() = type

    companion object {
        val Error = CfirMatchPattern(ConeErrorType(ConeSimpleDiagnostic("pattern error")), CfirMatchPatternKind.Error, null)

        fun wild(type: ConeCangJieType = ConeErrorType(ConeSimpleDiagnostic("unknown"))): CfirMatchPattern =
            CfirMatchPattern(type, CfirMatchPatternKind.Wild, null)
    }
}

sealed class CfirMatchPatternKind {
    data object Error : CfirMatchPatternKind()
    data object Wild : CfirMatchPatternKind()
    data class Binding(val name: String) : CfirMatchPatternKind()
    data class Type(val type: ConeCangJieType, val name: String?) : CfirMatchPatternKind()
    data class Const(val value: CfirConstantValue) : CfirMatchPatternKind()
    data class Tuple(val subPatterns: List<CfirMatchPattern>) : CfirMatchPatternKind()
    data class Enum(
        val enumClassId: ClassId,
        val entryName: String,
        val subPatterns: List<CfirMatchPattern>,
    ) : CfirMatchPatternKind()
}

sealed class CfirConstantValue : Comparable<CfirConstantValue> {
    abstract fun toText(): String

    data object UnitConst : CfirConstantValue() {
        override fun toText(): String = "Unit"
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            UnitConst -> 0
            else -> throw IllegalArgumentException("incompatible const comparison: Unit vs $other")
        }
    }

    data class BooleanConst(val value: Boolean) : CfirConstantValue() {
        override fun toText(): String = value.toString()
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is BooleanConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: Boolean vs $other")
        }
    }

    data class SignedIntConst(val value: Long) : CfirConstantValue() {
        override fun toText(): String = value.toString()
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is SignedIntConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: Int vs $other")
        }
    }

    data class UnsignedIntConst(val value: ULong) : CfirConstantValue() {
        override fun toText(): String = value.toString()
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is UnsignedIntConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: UInt vs $other")
        }
    }

    data class RuneConst(val value: Int) : CfirConstantValue() {
        override fun toText(): String = "Rune(0x${value.toString(16)})"
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is RuneConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: Rune vs $other")
        }
    }

    data class StringConst(val value: String) : CfirConstantValue() {
        override fun toText(): String = value
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is StringConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: String vs $other")
        }
    }

    companion object {
        fun fromLiteral(literal: CfirLiteralExpression, fallbackType: ConeCangJieType?): CfirConstantValue? {
            return when (literal.kind) {
                CfirLiteralKind.BOOLEAN -> (literal.value as? Boolean)?.let(::BooleanConst)
                CfirLiteralKind.INT -> fromIntLiteral(literal.value, fallbackType)
                CfirLiteralKind.RUNE -> fromRuneLiteral(literal.value)
                CfirLiteralKind.STRING -> (literal.value as? String)?.let(::StringConst)
                CfirLiteralKind.UNIT -> UnitConst
                else -> null
            }
        }

        private fun fromRuneLiteral(value: Any?): CfirConstantValue? {
            val codePoint = when (value) {
                is Char -> value.code
                is Int -> value
                is Long -> value.toInt()
                else -> null
            } ?: return null
            return RuneConst(codePoint)
        }

        private fun fromIntLiteral(value: Any?, fallbackType: ConeCangJieType?): CfirConstantValue? {
            val primitive = fallbackType as? ConePrimitiveType
            val unsigned = primitive?.kind in setOf(
                PrimitiveTypeKind.UINT8,
                PrimitiveTypeKind.UINT16,
                PrimitiveTypeKind.UINT32,
                PrimitiveTypeKind.UINT64,
                PrimitiveTypeKind.UINT_NATIVE,
            )
            return if (unsigned) {
                when (value) {
                    is UByte -> UnsignedIntConst(value.toULong())
                    is UShort -> UnsignedIntConst(value.toULong())
                    is UInt -> UnsignedIntConst(value.toULong())
                    is ULong -> UnsignedIntConst(value)
                    is Byte -> UnsignedIntConst(value.toULong())
                    is Short -> UnsignedIntConst(value.toULong())
                    is Int -> UnsignedIntConst(value.toULong())
                    is Long -> UnsignedIntConst(value.toULong())
                    else -> null
                }
            } else {
                when (value) {
                    is Byte -> SignedIntConst(value.toLong())
                    is Short -> SignedIntConst(value.toLong())
                    is Int -> SignedIntConst(value.toLong())
                    is Long -> SignedIntConst(value)
                    is UByte -> SignedIntConst(value.toLong())
                    is UShort -> SignedIntConst(value.toLong())
                    is UInt -> SignedIntConst(value.toLong())
                    is ULong -> SignedIntConst(value.toLong())
                    else -> null
                }
            }
        }
    }
}

sealed class CfirConstructor {
    open fun arity(type: ConeCangJieType): Int = when (val patternType = type.expandedPatternEnumType()) {
        is ConeTupleType if this is Single -> patternType.elementTypes.size
        is ConeEnumType if this is Enum -> arityHint
        is ConeClassLikeType if this is Enum && patternType.classId == StdlibClassIds.Option ->
            stdlibOptionConstructorArity(entryName) ?: 0
        else -> 0
    }

    open fun subTypes(type: ConeCangJieType): List<ConeCangJieType> = when (val patternType = type.expandedPatternEnumType()) {
        is ConeTupleType if this is Single -> patternType.elementTypes
        is ConeEnumType if this is Enum -> List(arityHint) { ConeErrorType(ConeSimpleDiagnostic("enum constructor argument")) }
        is ConeClassLikeType if this is Enum && patternType.classId == StdlibClassIds.Option ->
            patternType.stdlibOptionConstructorSubTypes(entryName)
        else -> emptyList()
    }

    open fun coveredByRange(
        from: CfirConstantValue,
        to: CfirConstantValue,
        included: Boolean,
    ): Boolean = false

    data class Enum(val enumClassId: ClassId, val entryName: String, val arityHint: Int = 0) : CfirConstructor()
    data class Type(val type: ConeCangJieType) : CfirConstructor()

    data object Single : CfirConstructor() {
        override fun coveredByRange(from: CfirConstantValue, to: CfirConstantValue, included: Boolean): Boolean = true
    }

    data class ConstantValue(val value: CfirConstantValue) : CfirConstructor() {
        override fun coveredByRange(from: CfirConstantValue, to: CfirConstantValue, included: Boolean): Boolean {
            return if (included) value in from..to else value in from..<to
        }
    }

    companion object {
        fun allConstructors(type: ConeCangJieType, session: CfirSession): List<CfirConstructor> = when (val patternType = type.expandedPatternEnumType()) {
            is ConePrimitiveType -> when (patternType.kind) {
                PrimitiveTypeKind.BOOLEAN -> listOf(
                    ConstantValue(CfirConstantValue.BooleanConst(true)),
                    ConstantValue(CfirConstantValue.BooleanConst(false)),
                )
                PrimitiveTypeKind.UNIT -> listOf(ConstantValue(CfirConstantValue.UnitConst))
                else -> listOf(Single)
            }

            is ConeEnumType -> {
                val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(patternType.classId)
                val klass = classSymbol?.takeIf { it.isBound }?.cfir ?: return emptyList()
                klass.declarations
                    .filterIsInstance<CfirEnumConstructor>()
                    .map { Enum(patternType.classId, it.name.asString(), arityHint = it.payloadArity()) }
            }

            is ConeClassLikeType if patternType.classId == StdlibClassIds.Option -> listOf(
                Enum(StdlibClassIds.Option, OPTION_SOME_CONSTRUCTOR_NAME, arityHint = 1),
                Enum(StdlibClassIds.Option, OPTION_NONE_CONSTRUCTOR_NAME, arityHint = 0),
            )

            else -> listOf(Single)
        }
    }
}

fun CfirMatchExpression.calculateMatrix(subjectType: ConeCangJieType): CfirMatrix {
    return branches.flatMap { branch ->
        convertPattern(branch.pattern, subjectType).map { listOf(it) }
    }
}

fun CfirPattern.calculateMatrix(expectedType: ConeCangJieType): CfirMatrix =
    convertPattern(this, expectedType).map { listOf(it) }

fun convertPattern(pattern: CfirPattern, expectedType: ConeCangJieType): List<CfirMatchPattern> {
    return when (pattern) {
        is CfirOrPattern -> pattern.alternatives.flatMap { convertPattern(it, expectedType) }
        is CfirWildcardPattern -> listOf(CfirMatchPattern.wild(expectedType))
        is CfirVarOrEnumPattern -> listOf(CfirMatchPattern.Error.copy(cfirPattern = pattern))
        is CfirBindingPattern -> {
            val nested = pattern.nestedPattern
            if (nested == null) {
                listOf(
                    CfirMatchPattern(
                        expectedType,
                        CfirMatchPatternKind.Binding(pattern.name.asString()),
                        pattern,
                    )
                )
            } else {
                convertPattern(nested, expectedType)
            }
        }

        is CfirConstPattern -> {
            val literal = pattern.expression as? CfirLiteralExpression
            val const = literal?.let { CfirConstantValue.fromLiteral(it, expectedType) }
            if (const != null) {
                listOf(CfirMatchPattern(expectedType, CfirMatchPatternKind.Const(const), pattern))
            } else {
                listOf(CfirMatchPattern.Error.copy(cfirPattern = pattern))
            }
        }

        is CfirEnumPattern -> {
            val patternType = expectedType.expandedPatternEnumType()
            val enumClassId = patternType.patternEnumClassId()
            val entryName = (pattern.constructorReference as? CfirNamedReference)?.name?.asString()
            if (enumClassId == null || entryName == null) {
                listOf(CfirMatchPattern.Error.copy(cfirPattern = pattern))
            } else {
                val constructor = CfirConstructor.Enum(enumClassId, entryName, pattern.arguments.size)
                val argumentTypes = constructor.subTypes(patternType)
                val subPatterns = pattern.arguments.mapIndexed { index, sub ->
                    val subType = argumentTypes.getOrNull(index)
                        ?: ConeErrorType(ConeSimpleDiagnostic("enum arg[$index]"))
                    convertPattern(sub, subType).firstOrNull() ?: CfirMatchPattern.wild(subType)
                }
                listOf(
                    CfirMatchPattern(
                        expectedType,
                        CfirMatchPatternKind.Enum(enumClassId, entryName, subPatterns),
                        pattern,
                    )
                )
            }
        }

        is CfirTuplePattern -> {
            val tupleType = expectedType as? ConeTupleType
            val subPatterns = pattern.elements.mapIndexed { index, sub ->
                val elementType = tupleType?.elementTypes?.getOrNull(index)
                    ?: ConeErrorType(ConeSimpleDiagnostic("tuple element[$index]"))
                convertPattern(sub, elementType).firstOrNull() ?: CfirMatchPattern.wild(elementType)
            }
            listOf(CfirMatchPattern(expectedType, CfirMatchPatternKind.Tuple(subPatterns), pattern))
        }

        is CfirTypePattern -> {
            val resolvedType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType ?: expectedType
            listOf(
                CfirMatchPattern(
                    expectedType,
                    CfirMatchPatternKind.Type(resolvedType, pattern.bindingName?.asString()),
                    pattern,
                )
            )
        }

        is CfirExpressionPattern -> {
            val expr = pattern.expression as? CfirLiteralExpression
            val const = expr?.let { CfirConstantValue.fromLiteral(it, expectedType) }
            if (const != null) {
                listOf(CfirMatchPattern(expectedType, CfirMatchPatternKind.Const(const), pattern))
            } else {
                listOf(CfirMatchPattern.Error.copy(cfirPattern = pattern))
            }
        }

        else -> listOf(CfirMatchPattern.Error.copy(cfirPattern = pattern))
    }
}

fun isSameType(a: ConeCangJieType, b: ConeCangJieType): Boolean {
    return a == b
}

fun inferExpressionType(
    expression: CfirExpression?,
    fallback: ConeCangJieType = ConeErrorType(ConeSimpleDiagnostic("unknown")),
): ConeCangJieType {
    return expression?.coneTypeOrNull ?: fallback
}

private fun ConeCangJieType.expandedPatternEnumType(): ConeCangJieType = when (this) {
    is ConeTypeAliasType -> expandedType?.expandedPatternEnumType() ?: this
    else -> this
}

private fun ConeCangJieType.patternEnumClassId(): ClassId? = when (this) {
    is ConeEnumType -> classId
    is ConeClassLikeType -> classId.takeIf { it == StdlibClassIds.Option }
    else -> null
}

private fun stdlibOptionConstructorArity(entryName: String): Int? = when (entryName) {
    OPTION_SOME_CONSTRUCTOR_NAME -> 1
    OPTION_NONE_CONSTRUCTOR_NAME -> 0
    else -> null
}

/**
 * `Option<T>` 以 class-like 类型进入矩阵模型时，仍按官方 enum payload 投影子模式类型。
 */
private fun ConeClassLikeType.stdlibOptionConstructorSubTypes(entryName: String): List<ConeCangJieType> = when (entryName) {
    OPTION_SOME_CONSTRUCTOR_NAME -> typeArguments.singleOrNull()?.type?.let(::listOf) ?: emptyList()
    OPTION_NONE_CONSTRUCTOR_NAME -> emptyList()
    else -> emptyList()
}

fun collectEnumConstructorNames(type: ConeEnumType, context: MatchExhaustivenessContext): List<String> {
    val classSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(type.classId) ?: return emptyList()
    if (!classSymbol.isBound) return emptyList()
    return classSymbol.cfir.declarations
        .filterIsInstance<CfirEnumConstructor>()
        .map { it.name.asString() }
}
