package org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor as CfirClassConstructor
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.match.CfirConstructor
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.isNonExhaustiveEnum
import org.cangnova.cangjie.cfir.resolve.match.isTypePatternOrdinarySubtypeOf
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.descriptors.Visibilities

/**
 * Maranget 模式 useful/穷尽性通用检查器。
 *
 * 该 checker 作为 specialized checker 无法覆盖时的兜底通用算法实现。
 */
class MarangetChecker : ExhaustivenessChecker {
    /** 当前 checker 来源。 */
    override val source: CheckSource = CheckSource.MARANGET

    /** 当前 checker 优先级最低，作为通用算法兜底。 */
    override val priority: Int = 100

    /** Maranget checker 对所有类型适用。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean = true

    /** 使用 wildcard 探测矩阵是否仍存在 useful 行，从而判断是否未穷尽。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): ExhaustivenessResult {
        if (!matrix.isWellTyped()) return ExhaustivenessResult.Error("matrix is not well typed")

        val wildPattern = CfirMatchPattern.wild(type)
        val useful = isUseful(
            matrix = matrix,
            patterns = listOf(wildPattern),
            withWitness = true,
            context = context,
            isTopLevel = true,
        )

        return when (useful) {
            is Usefulness.UsefulWithWitness -> {
                val missing = useful.witnesses.mapNotNull { it.patterns.singleOrNull() }
                ExhaustivenessResult.NonExhaustive(missing, source)
            }

            is Usefulness.Useful -> ExhaustivenessResult.NonExhaustive(emptyList(), source)
            Usefulness.Useless -> ExhaustivenessResult.Exhaustive
        }
    }

    /**
     * 判断 [patterns] 对当前矩阵是否 useful。
     *
     * @param withWitness 是否生成缺失模式 witness。
     * @param isTopLevel 当前递归是否位于最外层，用于控制缺失构造器展示。
     */
    fun isUseful(
        matrix: CfirMatrix,
        patterns: List<CfirMatchPattern>,
        withWitness: Boolean,
        context: MatchExhaustivenessContext,
        isTopLevel: Boolean,
    ): Usefulness {
        /**
         * 尝试按候选构造器逐个特化。
         */
        fun expandConstructors(constructors: List<CfirConstructor>, type: ConeCangJieType): Usefulness {
            for (constructor in constructors) {
                val result = isUsefulSpecialized(matrix, patterns, constructor, type, withWitness, context)
                if (result.isUseful) return result
            }
            return Usefulness.Useless
        }

        if (patterns.isEmpty()) {
            return if (matrix.isEmpty()) {
                if (withWitness) Usefulness.UsefulWithWitness.Empty else Usefulness.Useful
            } else {
                Usefulness.Useless
            }
        }

        val pattern = patterns.first()
        val type = matrix.firstColumnType ?: pattern.ergonomicType
        if (pattern.isUnreachableTypePattern(context)) return Usefulness.Useless
        if (pattern.kind is CfirMatchPatternKind.Type) {
            val wildcard = listOf(CfirMatchPattern.wild(type))
            if (isUseful(matrix, wildcard, withWitness = false, context, isTopLevel = false) is Usefulness.Useless) {
                return Usefulness.Useless
            }
        }
        val constructors = pattern.constructors
        if (constructors.isNotEmpty()) return expandConstructors(constructors, type)

        val usedConstructors = matrix.firstColumn.flatMap { it.constructors }.toSet()
        val allConstructors = CfirConstructor.allConstructors(type, context.session)
        val missingConstructors = allConstructors.minus(usedConstructors)

        val isPrivatelyEmpty = allConstructors.isEmpty()
        val isDeclaredNonExhaustive = type.isTyAdt() && hasNonExhaustiveAttribute(type, context)
        val isNonExhaustive = isPrivatelyEmpty || isDeclaredNonExhaustive

        if (missingConstructors.isEmpty() && !isNonExhaustive) {
            return expandConstructors(allConstructors, type)
        }

        val wildcardRows = matrix.filter { row ->
            when (val kind = row.firstOrNull()?.kind) {
                CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> true
                is CfirMatchPatternKind.Type -> kind.type == type
                else -> false
            }
        }

        val wildcardSubmatrix = wildcardRows.map { it.drop(1) }
        val remainingPatterns = patterns.drop(1)
        val res = isUseful(wildcardSubmatrix, remainingPatterns, withWitness, context, isTopLevel = false)

        if (res is Usefulness.UsefulWithWitness) {
            val reportConstructors = isTopLevel && !type.isIntegerLike()
            val newWitness = if (!reportConstructors && (isNonExhaustive || usedConstructors.isEmpty())) {
                res.witnesses.map { witness ->
                    witness.patterns.add(CfirMatchPattern.wild(type))
                    witness
                }
            } else {
                res.witnesses.flatMap { witness ->
                    missingConstructors.map { constructor ->
                        witness.clone().pushWildConstructor(constructor, type)
                    }
                }
            }
            return Usefulness.UsefulWithWitness(newWitness)
        }

        return res
    }

    /**
     * 按单个构造器特化矩阵和待判断模式后递归计算 usefulness。
     */
    private fun isUsefulSpecialized(
        matrix: CfirMatrix,
        patterns: List<CfirMatchPattern>,
        constructor: CfirConstructor,
        type: ConeCangJieType,
        withWitness: Boolean,
        context: MatchExhaustivenessContext,
    ): Usefulness {
        val newPatterns = RowSpecializer.specializeRow(patterns, constructor, type, context) ?: return Usefulness.Useless
        val newMatrix = matrix.mapNotNull { row -> RowSpecializer.specializeRow(row, constructor, type, context) }
        val useful = isUseful(newMatrix, newPatterns, withWitness, context, isTopLevel = false)
        return when (useful) {
            is Usefulness.UsefulWithWitness ->
                Usefulness.UsefulWithWitness(useful.witnesses.map { it.applyConstructor(constructor, type) })

            else -> useful
        }
    }

    /**
     * 类型是否声明为非穷尽。
     */
    private fun hasNonExhaustiveAttribute(type: ConeCangJieType, context: MatchExhaustivenessContext): Boolean =
        type.isNonExhaustiveEnum(context.session)

    /** 单例实例。 */
    companion object {
        /** 默认 Maranget checker 实例。 */
        val INSTANCE = MarangetChecker()
    }
}

/**
 * 判断类型是否属于代数数据类型形态。
 */
private fun ConeCangJieType.isTyAdt(): Boolean = this is ConeEnumType || this is ConeStructType

/**
 * 判断类型是否为整数 primitive。
 */
private fun ConeCangJieType.isIntegerLike(): Boolean =
    this is ConePrimitiveType && kind.isInteger

/**
 * 对齐官方 `DestructedPattern::IsUnreachableTypePattern`：
 * 只有 `Nothing` 或双方都是封闭类型且互无子类型关系时，type pattern 才能静态判死。
 * 开放 class/interface 和仍含未解析泛型分量的类型必须保留运行期可达可能性。
 */
private fun CfirMatchPattern.isUnreachableTypePattern(context: MatchExhaustivenessContext): Boolean {
    val patternType = (kind as? CfirMatchPatternKind.Type)?.type ?: return false
    if (patternType.isNothing) return true
    val goalType = type
    if (goalType.hasOpenTypeShape() || patternType.hasOpenTypeShape()) return false
    if (!goalType.isSealedLikeForTypePattern(context) || !patternType.isSealedLikeForTypePattern(context)) return false

    val goalIsSubtype = goalType.isTypePatternOrdinarySubtypeOf(patternType, context.session)
    val patternIsSubtype = patternType.isTypePatternOrdinarySubtypeOf(goalType, context.session)
    return !goalIsSubtype && !patternIsSubtype
}

/**
 * 判断类型形状是否仍包含未解析的泛型或错误分量。
 *
 * `Option<Int64>` 这类 concrete 实例化是封闭形状；只有 `Option<T>`、fresh type variable
 * 或错误类型才保留运行期开放性。不能仅因存在 type argument 就放弃静态不相交判定。
 */
private fun ConeCangJieType.hasOpenTypeShape(): Boolean = when (this) {
    is ConeTypeParameterType,
    is ConeTypeVariableType,
    is ConeStubType,
    is ConePlaceholderType,
    is ConeQuestType,
    is ConeErrorType,
    -> true

    is ConeTypeAliasType -> typeArguments.any { it.type.hasOpenTypeShape() } ||
            expandedType?.hasOpenTypeShape() == true
    is ConeLookupTagBasedType -> typeArguments.any { it.type.hasOpenTypeShape() }
    is ConeFunctionType -> parameterTypes.any { it.hasOpenTypeShape() } || returnType.hasOpenTypeShape()
    is ConeTupleType -> elementTypes.any { it.hasOpenTypeShape() }
    is ConeVArrayType -> elementType.hasOpenTypeShape()
    is ConePointerType -> pointeeType.hasOpenTypeShape()
    is ConeIntersectionType -> intersectedTypes.any { it.hasOpenTypeShape() } ||
            upperBoundForApproximation?.hasOpenTypeShape() == true
    is ConeUnionType -> unionTypes.any { it.hasOpenTypeShape() }
    else -> false
}

/**
 * 判断类型模式匹配中该类型是否可视为封闭类型。
 */
private fun ConeCangJieType.isSealedLikeForTypePattern(context: MatchExhaustivenessContext): Boolean {
    return when (this) {
        is ConePrimitiveType,
        is ConeEnumType,
        is ConeStructType,
        is ConeTupleType,
        is ConeFunctionType,
        -> true

        is ConeClassLikeType -> {
            val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return false
            if (!symbol.isBound) return false
            symbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
            val declaration = symbol.cfir as? CfirMemberDeclaration ?: return false
            declaration.isSealedLikeClassLike()
        }

        else -> false
    }
}

/**
 * 对齐官方 `IsSealedLike`：interface 只有 sealed/private 才封闭；
 * class 还可因没有 public/protected 构造器而封闭。普通非 open interface 仍可被外部实现，
 * 不能参与 type-pattern 静态判死。
 */
private fun CfirMemberDeclaration.isSealedLikeClassLike(): Boolean {
    if (status.isSealed || Visibilities.isPrivate(status.visibility)) return true
    return when (this) {
        is CfirClass -> !hasVisibleConstructor()
        is CfirInterface -> false
        else -> true
    }
}

/** 判断 class 是否存在官方意义上的可见构造器。 */
private fun CfirClass.hasVisibleConstructor(): Boolean =
    declarations.filterIsInstance<CfirClassConstructor>().any { constructor ->
        constructor.status.visibility === Visibilities.Public ||
            constructor.status.visibility === Visibilities.Protected
    }
