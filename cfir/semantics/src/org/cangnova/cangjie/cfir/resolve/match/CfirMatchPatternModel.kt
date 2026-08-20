/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.match

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.enumPatternConstructorAccessOrNull
import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
import org.cangnova.cangjie.cfir.declarations.payloadArity
import org.cangnova.cangjie.cfir.declarations.substitutedPayloadParameterTypes
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId

/**
 * match 穷尽性算法使用的模式矩阵。
 */
typealias CfirMatrix = List<List<CfirMatchPattern>>

/** 标准库 `Option.Some` 构造器名称。 */
private const val OPTION_SOME_CONSTRUCTOR_NAME = "Some"

/** 标准库 `Option.None` 构造器名称。 */
private const val OPTION_NONE_CONSTRUCTOR_NAME = "None"

/**
 * 供穷尽性算法消费的规范化 match 模式。
 *
 * @property type 当前模式匹配的目标类型。
 * @property kind 模式的规范化种类。
 * @property cfirPattern 原始 CFIR 模式；为空表示合成模式。
 */
data class CfirMatchPattern(
    /**
     * 当前模式匹配的目标类型。
     */
    val type: ConeCangJieType,
    /**
     * 模式的规范化种类。
     */
    val kind: CfirMatchPatternKind,
    /**
     * 原始 CFIR 模式；为空表示合成模式。
     */
    val cfirPattern: CfirPattern? = null,
) {
    /**
     * 返回该模式的可读文本。
     */
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

    /** 当前模式可覆盖的构造器集合。 */
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

    /** 算法使用的人体工学类型，当前等同于 [type]。 */
    val ergonomicType: ConeCangJieType
        get() = type

    /**
     * 常用模式构造工具。
     */
    companion object {
        /** 错误模式占位。 */
        val Error = CfirMatchPattern(ConeErrorType(ConeSimpleDiagnostic("pattern error")), CfirMatchPatternKind.Error, null)

        /**
         * 创建通配模式。
         */
        fun wild(type: ConeCangJieType = ConeErrorType(ConeSimpleDiagnostic("unknown"))): CfirMatchPattern =
            CfirMatchPattern(type, CfirMatchPatternKind.Wild, null)
    }
}

/**
 * 规范化 match 模式种类。
 */
sealed class CfirMatchPatternKind {
    /** 错误模式。 */
    data object Error : CfirMatchPatternKind()

    /** 通配模式。 */
    data object Wild : CfirMatchPatternKind()

    /**
     * 绑定模式。
     *
     * @property name 绑定名称。
     */
    data class Binding(val name: String) : CfirMatchPatternKind()

    /**
     * 类型模式。
     *
     * @property type 模式声明的类型。
     * @property name 类型模式上的绑定名称。
     */
    data class Type(val type: ConeCangJieType, val name: String?) : CfirMatchPatternKind()

    /**
     * 常量模式。
     *
     * @property value 常量值。
     */
    data class Const(val value: CfirConstantValue) : CfirMatchPatternKind()

    /**
     * 元组模式。
     *
     * @property subPatterns 元组各元素子模式。
     */
    data class Tuple(val subPatterns: List<CfirMatchPattern>) : CfirMatchPatternKind()

    /**
     * enum 构造器模式。
     *
     * @property enumClassId enum 类型 classId。
     * @property entryName enum 构造器名称。
     * @property subPatterns 构造器 payload 子模式。
     */
    data class Enum(
        /**
         * enum 类型 classId。
         */
        val enumClassId: ClassId,
        /**
         * enum 构造器名称。
         */
        val entryName: String,
        /**
         * 构造器 payload 子模式。
         */
        val subPatterns: List<CfirMatchPattern>,
    ) : CfirMatchPatternKind()
}

/**
 * 穷尽性算法可比较的常量值。
 */
sealed class CfirConstantValue : Comparable<CfirConstantValue> {
    /**
     * 渲染常量模式文本。
     */
    abstract fun toText(): String

    /** Unit 常量。 */
    data object UnitConst : CfirConstantValue() {
        /** Unit 常量文本。 */
        override fun toText(): String = "Unit"
        /** Unit 常量只能与 Unit 比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            UnitConst -> 0
            else -> throw IllegalArgumentException("incompatible const comparison: Unit vs $other")
        }
    }

    /**
     * Bool 常量。
     *
     * @property value 布尔值。
     */
    data class BooleanConst(val value: Boolean) : CfirConstantValue() {
        /** 布尔常量文本。 */
        override fun toText(): String = value.toString()
        /** 布尔常量比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is BooleanConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: Boolean vs $other")
        }
    }

    /**
     * 有符号整数常量。
     *
     * @property value 统一转换后的 Long 值。
     */
    data class SignedIntConst(val value: Long) : CfirConstantValue() {
        /** 有符号整数文本。 */
        override fun toText(): String = value.toString()
        /** 有符号整数比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is SignedIntConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: Int vs $other")
        }
    }

    /**
     * 无符号整数常量。
     *
     * @property value 统一转换后的 ULong 值。
     */
    data class UnsignedIntConst(val value: ULong) : CfirConstantValue() {
        /** 无符号整数文本。 */
        override fun toText(): String = value.toString()
        /** 无符号整数比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is UnsignedIntConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: UInt vs $other")
        }
    }

    /**
     * Rune 常量。
     *
     * @property value Unicode code point。
     */
    data class RuneConst(val value: Int) : CfirConstantValue() {
        /** Rune 常量文本。 */
        override fun toText(): String = "Rune(0x${value.toString(16)})"
        /** Rune 常量比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is RuneConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: Rune vs $other")
        }
    }

    /**
     * 字符串常量。
     *
     * @property value 字符串字面量值。
     */
    data class StringConst(val value: String) : CfirConstantValue() {
        /** 字符串常量文本。 */
        override fun toText(): String = value
        /** 字符串常量比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is StringConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: String vs $other")
        }
    }

    /**
     * 常量值构造工具。
     */
    companion object {
        /**
         * 从 CFIR 字面量表达式恢复常量值。
         */
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

        /**
         * 从 rune 字面量值恢复常量。
         */
        private fun fromRuneLiteral(value: Any?): CfirConstantValue? {
            val codePoint = when (value) {
                is Char -> value.code
                is Int -> value
                is Long -> value.toInt()
                else -> null
            } ?: return null
            return RuneConst(codePoint)
        }

        /**
         * 从整数字面量值恢复有符号或无符号常量。
         */
        private fun fromIntLiteral(value: Any?, fallbackType: ConeCangJieType?): CfirConstantValue? {
            val primitive = fallbackType as? ConePrimitiveType
            val unsigned = primitive?.kind in setOf(
                PrimitiveTypeKind.UINT8,
                PrimitiveTypeKind.UINT16,
                PrimitiveTypeKind.UINT32,
                PrimitiveTypeKind.UINT64,
                PrimitiveTypeKind.UINT_NATIVE,
            )
            val parsed = CfirIntConstantEvalUtils.parseIntLiteralValue(value) ?: return null
            return if (unsigned) {
                parsed.value.toString().toULongOrNull()?.let(::UnsignedIntConst)
            } else {
                try {
                    SignedIntConst(parsed.value.longValueExact())
                } catch (_: ArithmeticException) {
                    null
                }
            }
        }
    }
}

/**
 * 穷尽性算法中的构造器抽象。
 */
sealed class CfirConstructor {
    /**
     * 构造器在指定类型下的 payload 元数。
     */
    open fun arity(type: ConeCangJieType): Int = when (val patternType = type) {
        is ConeTupleType if this is Single -> patternType.elementTypes.size
        is ConeEnumType if this is Enum -> payloadTypes.size.takeIf { it != 0 } ?: arityHint
        is ConeClassLikeType if this is Enum && patternType.classId == StdlibClassIds.Option ->
            stdlibOptionConstructorArity(entryName) ?: 0
        else -> 0
    }

    /**
     * 构造器在指定类型下的 payload 子类型列表。
     */
    open fun subTypes(type: ConeCangJieType): List<ConeCangJieType> = when (val patternType = type) {
        is ConeTupleType if this is Single -> patternType.elementTypes
        is ConeEnumType if this is Enum -> payloadTypes.ifEmpty {
            List(arityHint) { ConeErrorType(ConeSimpleDiagnostic("enum constructor argument")) }
        }
        is ConeClassLikeType if this is Enum && patternType.classId == StdlibClassIds.Option ->
            patternType.stdlibOptionConstructorSubTypes(entryName)
        else -> emptyList()
    }

    /**
     * 当前构造器是否被常量区间覆盖。
     */
    open fun coveredByRange(
        from: CfirConstantValue,
        to: CfirConstantValue,
        included: Boolean,
    ): Boolean = false

    /**
     * enum 构造器。
     *
     * @property enumClassId enum 类型 classId。
     * @property entryName enum entry 名称。
     * @property arityHint 构造器 payload 元数。
     */
    class Enum(
        /**
         * enum 类型 classId。
         */
        val enumClassId: ClassId,
        /**
         * enum entry 名称。
         */
        val entryName: String,
        /**
         * 构造器 payload 元数提示。
         */
        val arityHint: Int = 0,
        /**
         * 当前 enum use-site 类型下替换后的 payload 类型。
         */
        val payloadTypes: List<ConeCangJieType> = emptyList(),
    ) : CfirConstructor() {
        /**
         * enum 构造器身份只由声明入口和 payload 元数决定。
         *
         * payloadTypes 是 use-site 下的展开信息，只服务子模式类型投影，不参与覆盖关系比较。
         */
        private val identityArity: Int
            get() = payloadTypes.size.takeIf { it != 0 } ?: arityHint

        override fun equals(other: Any?): Boolean =
            other is Enum &&
                enumClassId == other.enumClassId &&
                entryName == other.entryName &&
                identityArity == other.identityArity

        override fun hashCode(): Int {
            var result = enumClassId.hashCode()
            result = 31 * result + entryName.hashCode()
            result = 31 * result + identityArity
            return result
        }

        override fun toString(): String =
            "Enum(enumClassId=$enumClassId, entryName=$entryName, arity=$identityArity)"
    }

    /**
     * 类型模式构造器。
     *
     * @property type 类型模式声明类型。
     */
    data class Type(val type: ConeCangJieType) : CfirConstructor()

    /** 单一构造器，用于元组、普通 class-like 或无法枚举的类型。 */
    data object Single : CfirConstructor() {
        /** 单一构造器总是被任意区间覆盖。 */
        override fun coveredByRange(from: CfirConstantValue, to: CfirConstantValue, included: Boolean): Boolean = true
    }

    /** 非穷尽 enum 的未知未来构造器。 */
    data object NonExhaustiveEnum : CfirConstructor()

    /**
     * 常量值构造器。
     *
     * @property value 常量值。
     */
    data class ConstantValue(val value: CfirConstantValue) : CfirConstructor() {
        /** 判断该常量是否落在给定区间内。 */
        override fun coveredByRange(from: CfirConstantValue, to: CfirConstantValue, included: Boolean): Boolean {
            return if (included) value in from..to else value in from..<to
        }
    }

    /**
     * 构造器枚举工具。
     */
    companion object {
        /**
         * 枚举指定类型所有可知构造器。
         */
        fun allConstructors(type: ConeCangJieType, session: CfirSession): List<CfirConstructor> =
            when (val patternType = type.expandedPatternEnumType(session) ?: type) {
            is ConePrimitiveType -> when (patternType.kind) {
                PrimitiveTypeKind.BOOLEAN -> listOf(
                    ConstantValue(CfirConstantValue.BooleanConst(true)),
                    ConstantValue(CfirConstantValue.BooleanConst(false)),
                )
                PrimitiveTypeKind.UNIT -> listOf(ConstantValue(CfirConstantValue.UnitConst))
                else -> listOf(Single)
            }

            is ConeEnumType -> {
                val enumDeclaration = session.enumDeclaration(patternType) ?: return emptyList()
                val enumConstructors = enumDeclaration.declarations
                    .filterIsInstance<CfirEnumConstructor>()
                    .map { constructor ->
                        Enum(
                            patternType.classId,
                            constructor.name.asString(),
                            arityHint = constructor.payloadArity(),
                            payloadTypes = constructor.substitutedPayloadParameterTypes(enumDeclaration, patternType),
                        )
                    }
                if (enumDeclaration.isNonExhaustive) enumConstructors + NonExhaustiveEnum else enumConstructors
            }

            is ConeClassLikeType if patternType.classId == StdlibClassIds.Option -> listOf(
                Enum(StdlibClassIds.Option, OPTION_SOME_CONSTRUCTOR_NAME, arityHint = 1),
                Enum(StdlibClassIds.Option, OPTION_NONE_CONSTRUCTOR_NAME, arityHint = 0),
            )

            else -> listOf(Single)
        }
    }
}

/**
 * 将 match 表达式转换为单列模式矩阵。
 */
fun CfirMatchExpression.calculateMatrix(subjectType: ConeCangJieType, session: CfirSession): CfirMatrix {
    return branches.flatMap { branch ->
        convertPattern(branch.pattern, subjectType, session).map { listOf(it) }
    }
}

/**
 * 将单个 CFIR 模式转换为单列模式矩阵。
 */
fun CfirPattern.calculateMatrix(expectedType: ConeCangJieType, session: CfirSession): CfirMatrix =
    convertPattern(this, expectedType, session).map { listOf(it) }

/**
 * 将 CFIR 模式规范化为穷尽性算法模式。
 */
fun convertPattern(
    pattern: CfirPattern,
    expectedType: ConeCangJieType,
    session: CfirSession,
): List<CfirMatchPattern> {
    return when (pattern) {
        is CfirOrPattern -> {
            if (pattern.bindingOccurrences().isNotEmpty()) {
                listOf(CfirMatchPattern(expectedType, CfirMatchPatternKind.Error, pattern))
            } else {
                pattern.alternatives.flatMap { convertPattern(it, expectedType, session) }
            }
        }
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
                convertPattern(nested, expectedType, session)
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
            val patternType = expectedType.expandedPatternEnumType(session) ?: expectedType
            val enumClassId = patternType.patternEnumClassId()
            val constructorAccess = pattern.constructorReference.enumPatternConstructorAccessOrNull()
            val ownerMatches = when (patternType) {
                is ConeEnumType -> {
                    val enumDeclaration = session.enumDeclaration(patternType)
                    enumDeclaration != null && constructorAccess?.matchesEnumOwner(enumDeclaration, patternType) == true
                }

                is ConeClassLikeType if patternType.classId == StdlibClassIds.Option ->
                    constructorAccess?.matchesStdlibOptionOwner(patternType) == true

                else -> false
            }
            val entryName = constructorAccess?.constructorName?.asString()
            if (enumClassId == null || entryName == null || !ownerMatches) {
                listOf(CfirMatchPattern.Error.copy(cfirPattern = pattern))
            } else {
                val payloadTypes = if (patternType is ConeEnumType) {
                    session.enumConstructorPayloadTypes(patternType, entryName, pattern.arguments.size)
                } else {
                    emptyList()
                }
                val constructor = CfirConstructor.Enum(
                    enumClassId,
                    entryName,
                    arityHint = pattern.arguments.size,
                    payloadTypes = payloadTypes,
                )
                val argumentTypes = constructor.subTypes(patternType)
                val subPatterns = pattern.arguments.mapIndexed { index, sub ->
                    val subType = argumentTypes.getOrNull(index)
                        ?: ConeErrorType(ConeSimpleDiagnostic("enum arg[$index]"))
                    convertPattern(sub, subType, session).firstOrNull() ?: CfirMatchPattern.wild(subType)
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
                convertPattern(sub, elementType, session).firstOrNull() ?: CfirMatchPattern.wild(elementType)
            }
            listOf(CfirMatchPattern(expectedType, CfirMatchPatternKind.Tuple(subPatterns), pattern))
        }

        is CfirTypePattern -> {
            val resolvedType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType ?: expectedType
            listOf(
                if (resolvedType.coversExpectedType(expectedType, session)) {
                    CfirMatchPattern(expectedType, CfirMatchPatternKind.Wild, pattern)
                } else {
                    CfirMatchPattern(
                        expectedType,
                        CfirMatchPatternKind.Type(resolvedType, pattern.bindingName?.asString()),
                        pattern,
                    )
                }
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

    }
}

/**
 * 判断两个 Cone 类型是否语义相同。
 *
 * 当前实现使用结构相等，后续如接入类型上下文等价关系可在此集中替换。
 */
fun isSameType(a: ConeCangJieType, b: ConeCangJieType): Boolean {
    return a == b
}

/**
 * 官方 `PatternUsefulness::FromTypePattern` 中，`goalTy <: patternTy` 的类型模式
 * 等价于 wildcard；例如 `match (x: Int64) { case _: ToString => ... }`。
 */
private fun ConeCangJieType.coversExpectedType(expectedType: ConeCangJieType, session: CfirSession): Boolean {
    if (isNothing) return false
    return expectedType.isTypePatternWildcardSubtypeOf(this, session)
}

/**
 * 推断表达式类型，缺失时返回 [fallback]。
 */
fun inferExpressionType(
    expression: CfirExpression?,
    fallback: ConeCangJieType = ConeErrorType(ConeSimpleDiagnostic("unknown")),
): ConeCangJieType {
    return expression?.coneTypeOrNull ?: fallback
}

/**
 * 取得可作为 pattern enum 分析对象的 classId。
 */
private fun ConeCangJieType.patternEnumClassId(): ClassId? = when (this) {
    is ConeEnumType -> classId
    is ConeClassLikeType -> classId.takeIf { it == StdlibClassIds.Option }
    else -> null
}

/**
 * 查询标准库 `Option` 构造器 payload 元数。
 */
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

/**
 * 收集 enum 类型声明中的构造器名称。
 */
fun collectEnumConstructorNames(type: ConeEnumType, context: MatchExhaustivenessContext): List<String> {
    return context.session.enumDeclaration(type)
        ?.declarations
        ?.filterIsInstance<CfirEnumConstructor>()
        ?.map { it.name.asString() }
        .orEmpty()
}

/**
 * 取得 enum 声明本体。payload 类型、构造器枚举和缺失模式渲染必须复用同一声明入口。
 */
fun CfirSession.enumDeclaration(type: ConeEnumType): CfirEnum? {
    val classSymbol = symbolProvider.getClassLikeSymbolByClassId(type.classId) ?: return null
    if (!classSymbol.isBound) return null
    return classSymbol.cfir as? CfirEnum
}

/**
 * 判断类型是否是带 `...` 的非穷尽 enum。
 */
fun ConeCangJieType.isNonExhaustiveEnum(session: CfirSession): Boolean {
    val enumType = expandedPatternEnumType(session) ?: this as? ConeEnumType ?: return false
    return session.enumDeclaration(enumType)?.isNonExhaustive == true
}

/**
 * 计算 enum constructor 在当前 enum use-site 类型下的 payload 类型。
 */
private fun CfirSession.enumConstructorPayloadTypes(
    enumType: ConeEnumType,
    entryName: String,
    arity: Int,
): List<ConeCangJieType> {
    val enumDeclaration = enumDeclaration(enumType) ?: return emptyList()
    val constructor = enumDeclaration.declarations
        .filterIsInstance<CfirEnumConstructor>()
        .firstOrNull { it.name.asString() == entryName && it.payloadArity() == arity }
        ?: return emptyList()
    return constructor.substitutedPayloadParameterTypes(enumDeclaration, enumType)
}
