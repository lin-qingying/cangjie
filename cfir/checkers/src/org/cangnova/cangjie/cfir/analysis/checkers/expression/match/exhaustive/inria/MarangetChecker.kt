package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.inria

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirConstructor
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/** 基于 Luc Maranget 有用性算法的通用 match 穷尽性 checker。 */
class MarangetChecker : ExhaustivenessChecker {
    /** 当前 checker 在穷尽性结果中使用的来源标记。 */
    override val source: CheckSource = CheckSource.MARANGET

    /** Maranget 通用 checker 的兜底调度优先级。 */
    override val priority: Int = 100

    /** 通用算法可作为所有类型的兜底穷尽性检查。 */
    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean = true

    /** 以通配符目标模式运行有用性判定，并将有用 witness 转换为缺失模式。 */
    override fun check(
        matrix: CfirMatrix,
        type: ConeCangJieType,
        context: CheckerContext,
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

    /** 判断给定模式向量相对于当前矩阵是否有用，可选择保留 witness 以生成诊断。 */
    fun isUseful(
        matrix: CfirMatrix,
        patterns: List<CfirMatchPattern>,
        withWitness: Boolean,
        context: CheckerContext,
        isTopLevel: Boolean,
    ): Usefulness {
        /** 依次尝试构造器特化，只要任一构造器产生有用结果即可返回。 */
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
        val constructors = pattern.constructors
        if (constructors.isNotEmpty()) return expandConstructors(constructors, type)

        val usedConstructors = matrix.firstColumn.flatMap { it.constructors }.toSet()
        val allConstructors = CfirConstructor.allConstructors(type, context.session)
        val missingConstructors = allConstructors.minus(usedConstructors)

        val isPrivatelyEmpty = allConstructors.isEmpty()
        val isDeclaredNonExhaustive = type.isTyAdt() && hasNonExhaustiveAttribute(type)
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

    /** 对单个构造器执行矩阵和目标模式特化，并把递归 witness 重新包装为该构造器模式。 */
    private fun isUsefulSpecialized(
        matrix: CfirMatrix,
        patterns: List<CfirMatchPattern>,
        constructor: CfirConstructor,
        type: ConeCangJieType,
        withWitness: Boolean,
        context: CheckerContext,
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

    /** 查询类型是否声明了非穷尽属性；当前 CFIR match checker 尚未接入该属性来源。 */
    private fun hasNonExhaustiveAttribute(type: ConeCangJieType): Boolean = false

    /** Maranget checker 的共享单例容器。 */
    companion object {
        /** 默认 Maranget 通用 checker 单例。 */
        val INSTANCE = MarangetChecker()
    }
}

/** 判断类型是否为当前算法按代数数据类型处理的 enum 或 struct。 */
private fun ConeCangJieType.isTyAdt(): Boolean = this is ConeEnumType || this is ConeStructType

/** 判断类型是否为需要抑制顶层构造器 witness 展示的整数 primitive。 */
private fun ConeCangJieType.isIntegerLike(): Boolean =
    this is ConePrimitiveType && kind.isInteger

/**
 * 对齐官方 `DestructedPattern::IsUnreachableTypePattern`：
 * 只有 `Nothing` 或双方都是封闭类型且互无子类型关系时，type pattern 才能静态判死。
 * 开放 class/interface 和携带类型实参的 class-like 类型必须保留运行期可达可能性。
 */
private fun CfirMatchPattern.isUnreachableTypePattern(context: CheckerContext): Boolean {
    val patternType = (kind as? CfirMatchPatternKind.Type)?.type ?: return false
    if (patternType.isNothing) return true
    val goalType = type
    if (goalType.hasOpenTypeShape() || patternType.hasOpenTypeShape()) return false
    if (!goalType.isSealedLikeForTypePattern(context) || !patternType.isSealedLikeForTypePattern(context)) return false

    val goalIsSubtype = AbstractTypeChecker.isSubtypeOf(context.session.typeContext, goalType, patternType) == true
    val patternIsSubtype = AbstractTypeChecker.isSubtypeOf(context.session.typeContext, patternType, goalType) == true
    return !goalIsSubtype && !patternIsSubtype
}

/** 判断类型形状是否包含运行期仍开放的类型实参或嵌套开放 tuple。 */
private fun ConeCangJieType.hasOpenTypeShape(): Boolean = when (this) {
    is ConeClassifierType -> typeArguments.isNotEmpty()
    is ConeTupleType -> elementTypes.any { it.hasOpenTypeShape() }
    else -> false
}

/** 判断类型是否可按封闭类型参与静态 type pattern 不可达性推断。 */
private fun ConeCangJieType.isSealedLikeForTypePattern(context: CheckerContext): Boolean {
    return when (this) {
        is ConePrimitiveType,
        is ConeEnumType,
        is ConeStructType,
        is ConeTupleType,
        -> true

        is ConeClassLikeType -> {
            val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return false
            if (!symbol.isBound) return false
            symbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
            val status = (symbol.cfir as? CfirMemberDeclaration)?.status ?: return false
            status.isSealed || !status.isOpen
        }

        else -> false
    }
}
