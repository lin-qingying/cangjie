package org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria

import org.cangnova.cangjie.cfir.resolve.match.CfirConstructor
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeStructType

class MarangetChecker : ExhaustivenessChecker {
    override val source: CheckSource = CheckSource.MARANGET
    override val priority: Int = 100

    override fun isApplicable(
        type: ConeCangJieType,
        patterns: List<CfirMatchPattern>,
        context: MatchExhaustivenessContext,
    ): Boolean = true

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

    fun isUseful(
        matrix: CfirMatrix,
        patterns: List<CfirMatchPattern>,
        withWitness: Boolean,
        context: MatchExhaustivenessContext,
        isTopLevel: Boolean,
    ): Usefulness {
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

    private fun isUsefulSpecialized(
        matrix: CfirMatrix,
        patterns: List<CfirMatchPattern>,
        constructor: CfirConstructor,
        type: ConeCangJieType,
        withWitness: Boolean,
        context: MatchExhaustivenessContext,
    ): Usefulness {
        val newPatterns = RowSpecializer.specializeRow(patterns, constructor, type) ?: return Usefulness.Useless
        val newMatrix = matrix.mapNotNull { row -> RowSpecializer.specializeRow(row, constructor, type) }
        val useful = isUseful(newMatrix, newPatterns, withWitness, context, isTopLevel = false)
        return when (useful) {
            is Usefulness.UsefulWithWitness ->
                Usefulness.UsefulWithWitness(useful.witnesses.map { it.applyConstructor(constructor, type) })

            else -> useful
        }
    }

    private fun hasNonExhaustiveAttribute(type: ConeCangJieType): Boolean = false

    companion object {
        val INSTANCE = MarangetChecker()
    }
}

private fun ConeCangJieType.isTyAdt(): Boolean = this is ConeEnumType || this is ConeStructType

private fun ConeCangJieType.isIntegerLike(): Boolean =
    this is org.cangnova.cangjie.cfir.types.ConePrimitiveType && kind.isInteger
