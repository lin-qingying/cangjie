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

package org.cangnova.cangjie.cfir.analysis.checkers.expression.match

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
import org.cangnova.cangjie.cfir.declarations.payloadArity
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.AbstractTypeChecker

/** match usefulness / exhaustiveness 算法使用的模式矩阵，一行表示一个分支的模式向量。 */
typealias CfirMatrix = List<List<CfirMatchPattern>>

/** 标准库 `Option.Some` 在模式矩阵中的构造器名称。 */
private const val OPTION_SOME_CONSTRUCTOR_NAME = "Some"
/** 标准库 `Option.None` 在模式矩阵中的构造器名称。 */
private const val OPTION_NONE_CONSTRUCTOR_NAME = "None"

/**
 * match 模式算法使用的归一化模式节点。
 *
 * 该模型把 CFIR pattern 规整成常量、类型、tuple、enum、绑定和通配等有限种类，
 * 供穷尽性、不可达分支和 witness 算法统一消费。
 */
data class CfirMatchPattern(
    /** 当前模式所处位置的 expected type。 */
    val type: ConeCangJieType,
    /** 归一化后的模式种类。 */
    val kind: CfirMatchPatternKind,
    /** 产生该模型节点的原始 CFIR pattern；合成通配符可为空。 */
    val cfirPattern: CfirPattern? = null,
) {
    /** 返回用于诊断和 missing case 展示的模式文本。 */
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

    /** 返回该模式在 usefulness 算法中显式覆盖的构造器集合。 */
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

    /** 对算法调用方暴露的人体工程学类型别名，当前等同于 [type]。 */
    val ergonomicType: ConeCangJieType get() = type

    companion object {
        /** 通用错误模式节点，用于无法可靠建模的原始 pattern。 */
        val Error = CfirMatchPattern(ConeErrorType(ConeSimpleDiagnostic("pattern error")), CfirMatchPatternKind.Error, null)

        /** 构造指定 expected type 下的通配模式节点。 */
        fun wild(type: ConeCangJieType = ConeErrorType(ConeSimpleDiagnostic("unknown"))): CfirMatchPattern =
            CfirMatchPattern(type, CfirMatchPatternKind.Wild, null)
    }
}

/** match 模式矩阵中支持的归一化模式种类。 */
sealed class CfirMatchPatternKind {
    /** 原始 pattern 无法转换或已经携带错误。 */
    data object Error : CfirMatchPatternKind()
    /** `_` 或语义上等价于 wildcard 的类型模式。 */
    data object Wild : CfirMatchPatternKind()
    /** 变量绑定模式。 */
    data class Binding(val name: String) : CfirMatchPatternKind()
    /** 类型模式，携带解析后的类型和可选绑定名称。 */
    data class Type(val type: ConeCangJieType, val name: String?) : CfirMatchPatternKind()
    /** 字面量常量模式。 */
    data class Const(val value: CfirConstantValue) : CfirMatchPatternKind()
    /** tuple 模式，按元素顺序保存子模式。 */
    data class Tuple(val subPatterns: List<CfirMatchPattern>) : CfirMatchPatternKind()
    /** enum / Option 构造器模式，按 payload 顺序保存子模式。 */
    data class Enum(
        /** enum 类的 ClassId。 */
        val enumClassId: ClassId,
        /** enum entry 的名称文本。 */
        val entryName: String,
        /** 构造器 payload 对应的子模式列表。 */
        val subPatterns: List<CfirMatchPattern>,
    ) : CfirMatchPatternKind()
}

/**
 * 可用于模式区间和常量构造器比较的常量值模型。
 *
 * 不同常量族之间不可比较；调用方必须只在同一常量族内执行区间或排序判断。
 */
sealed class CfirConstantValue : Comparable<CfirConstantValue> {
    /** 返回诊断和 missing case 展示使用的常量文本。 */
    abstract fun toText(): String

    /** `Unit` 常量值。 */
    data object UnitConst : CfirConstantValue() {
        /** 返回 `Unit` 文本。 */
        override fun toText(): String = "Unit"
        /** 仅允许与另一个 `Unit` 常量比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            UnitConst -> 0
            else -> throw IllegalArgumentException("incompatible const comparison: Unit vs $other")
        }
    }

    /** 布尔常量值。 */
    data class BooleanConst(val value: Boolean) : CfirConstantValue() {
        /** 返回布尔字面量文本。 */
        override fun toText(): String = value.toString()
        /** 仅允许与布尔常量比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is BooleanConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: Boolean vs $other")
        }
    }

    /** 有符号整数常量值。 */
    data class SignedIntConst(val value: Long) : CfirConstantValue() {
        /** 返回有符号整数文本。 */
        override fun toText(): String = value.toString()
        /** 仅允许与有符号整数常量比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is SignedIntConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: Int vs $other")
        }
    }

    /** 无符号整数常量值。 */
    data class UnsignedIntConst(val value: ULong) : CfirConstantValue() {
        /** 返回无符号整数文本。 */
        override fun toText(): String = value.toString()
        /** 仅允许与无符号整数常量比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is UnsignedIntConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: UInt vs $other")
        }
    }

    /** Rune 常量值，内部保存 Unicode code point。 */
    data class RuneConst(val value: Int) : CfirConstantValue() {
        /** 返回 `Rune(0x...)` 文本。 */
        override fun toText(): String = "Rune(0x${value.toString(16)})"
        /** 仅允许与 Rune 常量比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is RuneConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: Rune vs $other")
        }
    }

    /** 字符串常量值。 */
    data class StringConst(val value: String) : CfirConstantValue() {
        /** 返回字符串内容文本。 */
        override fun toText(): String = value
        /** 仅允许与字符串常量比较。 */
        override fun compareTo(other: CfirConstantValue): Int = when (other) {
            is StringConst -> value.compareTo(other.value)
            else -> throw IllegalArgumentException("incompatible const comparison: String vs $other")
        }
    }

    companion object {
        /**
         * 从 CFIR 字面量表达式构造常量值模型。
         *
         * 整数字面量会结合 [fallbackType] 判定有符号/无符号常量族，避免区间算法混淆不同数值域。
         */
        fun fromLiteral(literal: CfirLiteralExpression, fallbackType: ConeCangJieType?): CfirConstantValue? {
            return when (literal.kind) {
                CfirLiteralKind.BOOLEAN -> (literal.value as? Boolean)?.let(::BooleanConst)
                CfirLiteralKind.INT -> fromIntLiteral(literal.value, fallbackType)
                CfirLiteralKind.BYTE -> fromIntLiteral(literal.value, fallbackType)
                CfirLiteralKind.RUNE -> fromRuneLiteral(literal.value)
                CfirLiteralKind.STRING -> (literal.value as? String)?.let(::StringConst)
                CfirLiteralKind.UNIT -> UnitConst
                else -> null
            }
        }

        /** 将 rune 字面量运行时值转换成 code point 常量。 */
        private fun fromRuneLiteral(value: Any?): CfirConstantValue? {
            val codePoint = when (value) {
                is Char -> value.code
                is Int -> value
                is Long -> value.toInt()
                else -> null
            } ?: return null
            return RuneConst(codePoint)
        }

        /** 根据 expected/fallback primitive 类型把整数字面量转换为有符号或无符号常量族。 */
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

/**
 * usefulness 算法中的构造器抽象。
 *
 * 构造器可以来自 enum entry、类型测试、tuple/single 形状或常量值；
 * 它负责提供子模式 arity、子类型投影以及常量区间覆盖判定。
 */
sealed class CfirConstructor {
    /** 返回该构造器在指定类型下需要展开的子模式个数。 */
    open fun arity(type: ConeCangJieType): Int = when (val patternType = type) {
        is ConeTupleType if this is Single -> patternType.elementTypes.size
        is ConeEnumType if this is Enum -> arityHint
        is ConeClassLikeType if this is Enum && patternType.classId == StdlibClassIds.Option ->
            stdlibOptionConstructorArity(entryName) ?: 0
        else -> 0
    }

    /** 返回该构造器展开后每个子位置的 expected type。 */
    open fun subTypes(type: ConeCangJieType): List<ConeCangJieType> = when (val patternType = type) {
        is ConeTupleType if this is Single -> patternType.elementTypes
        is ConeEnumType if this is Enum -> List(arityHint) { ConeErrorType(ConeSimpleDiagnostic("enum constructor argument")) }
        is ConeClassLikeType if this is Enum && patternType.classId == StdlibClassIds.Option ->
            patternType.stdlibOptionConstructorSubTypes(entryName)
        else -> emptyList()
    }

    /** 判断常量构造器是否被给定区间覆盖；非区间构造器默认不覆盖。 */
    open fun coveredByRange(
        from: CfirConstantValue,
        to: CfirConstantValue,
        included: Boolean,
    ): Boolean = false

    /** enum 构造器，携带 enum classId、entry 名称和 payload arity 提示。 */
    data class Enum(val enumClassId: ClassId, val entryName: String, val arityHint: Int = 0) : CfirConstructor()
    /** 类型测试构造器。 */
    data class Type(val type: ConeCangJieType) : CfirConstructor()

    /** 单一构造器，用于没有可枚举构造器集合的普通类型或 tuple 外壳。 */
    data object Single : CfirConstructor() {
        /** 单一构造器视为覆盖任意常量区间。 */
        override fun coveredByRange(from: CfirConstantValue, to: CfirConstantValue, included: Boolean): Boolean = true
    }

    /** 字面量常量构造器。 */
    data class ConstantValue(val value: CfirConstantValue) : CfirConstructor() {
        /** 判断该常量值是否落在给定闭区间或半开区间内。 */
        override fun coveredByRange(from: CfirConstantValue, to: CfirConstantValue, included: Boolean): Boolean {
            return if (included) value in from..to else value in from..<to
        }
    }

    companion object {
        /**
         * 枚举指定类型在 match 算法中的全部构造器。
         *
         * Boolean/Unit 直接展开为常量构造器，enum 展开为声明中的 enum constructor，
         * 标准库 `Option` 以 `Some`/`None` 建模，其余类型使用 [Single]。
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

/** 将整个 `match` 表达式的每个分支 pattern 转换成一列模式矩阵。 */
fun CfirMatchExpression.calculateMatrix(subjectType: ConeCangJieType, session: CfirSession): CfirMatrix {
    return branches.flatMap { branch ->
        convertPattern(branch.pattern, subjectType, session).map { listOf(it) }
    }
}

/** 将单个 CFIR pattern 转换成以该 pattern 为唯一列的模式矩阵。 */
fun CfirPattern.calculateMatrix(expectedType: ConeCangJieType, session: CfirSession): CfirMatrix =
    convertPattern(this, expectedType, session).map { listOf(it) }

/**
 * 把 CFIR pattern 归一化为 match 算法模型节点列表。
 *
 * `or` pattern 会展开成多个备选模型；绑定、常量、enum、tuple、类型模式会根据 expected type
 * 递归计算子模式类型；无法安全建模的情况返回错误模式节点。
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
            val entryName = (pattern.constructorReference as? CfirNamedReference)?.name?.asString()
            if (enumClassId == null || entryName == null) {
                listOf(CfirMatchPattern.Error.copy(cfirPattern = pattern))
            } else {
                val constructor = CfirConstructor.Enum(enumClassId, entryName, pattern.arguments.size)
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

/** 当前 match 模型使用的类型相等判断入口。 */
fun isSameType(a: ConeCangJieType, b: ConeCangJieType): Boolean {
    return a == b
}

/**
 * 官方 `PatternUsefulness::FromTypePattern` 中，`goalTy <: patternTy` 的类型模式
 * 等价于 wildcard；例如 `match (x: Int64) { case _: ToString => ... }`。
 */
private fun ConeCangJieType.coversExpectedType(expectedType: ConeCangJieType, session: CfirSession): Boolean {
    if (isNothing) return false
    return AbstractTypeChecker.isSubtypeOf(session.typeContext, expectedType, this)
}

/** 从表达式上读取已解析类型；表达式或类型缺失时使用 [fallback]。 */
fun inferExpressionType(expression: CfirExpression?, fallback: ConeCangJieType = ConeErrorType(ConeSimpleDiagnostic("unknown"))): ConeCangJieType {
    return expression?.coneTypeOrNull ?: fallback
}

/** 返回类型在模式构造器模型中对应的 enum classId；标准库 `Option` 也按 enum 处理。 */
private fun ConeCangJieType.patternEnumClassId(): ClassId? = when (this) {
    is ConeEnumType -> classId
    is ConeClassLikeType -> classId.takeIf { it == StdlibClassIds.Option }
    else -> null
}

/** 返回标准库 `Option` 构造器的 payload arity。 */
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

/** 收集 enum 类型声明中的 constructor 名称，用于诊断和缺失分支展示。 */
fun collectEnumConstructorNames(type: ConeEnumType, context: CheckerContext): List<String> {
    val classSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(type.classId) ?: return emptyList()
    if (!classSymbol.isBound) return emptyList()
    return classSymbol.cfir.declarations
        .filterIsInstance<CfirEnumConstructor>()
        .map { it.name.asString() }
}
