package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.structural

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.collectEnumConstructorNames
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.CheckSource
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeEnumType

class NestedFlattenChecker : ExhaustivenessChecker {
    override val source: CheckSource = CheckSource.NESTED_FLATTEN
    override val priority: Int = 50

    private val maxNestingDepth = 3
    private val maxFlattenedConstructors = 64

    override fun isApplicable(
        type: ConeCangjieType,
        patterns: List<CfirMatchPattern>,
        context: CheckerContext,
    ): Boolean {
        val enumType = type as? ConeEnumType ?: return false
        val flattenedCount = estimateFlattenedConstructors(enumType, context, 0)
        return flattenedCount in 1..maxFlattenedConstructors
    }

    override fun check(
        matrix: CfirMatrix,
        type: ConeCangjieType,
        context: CheckerContext,
    ): ExhaustivenessResult {
        val enumType = type as? ConeEnumType ?: return ExhaustivenessResult.Skipped
        val flattenedPatterns = matrix.mapNotNull { row -> row.firstOrNull()?.let(::flattenPattern) }
        if (flattenedPatterns.isEmpty()) return ExhaustivenessResult.Skipped

        val allPaths = collectAllPaths(enumType, context, 0)
        if (allPaths.size > maxFlattenedConstructors) return ExhaustivenessResult.Skipped

        val indexByPath = allPaths.withIndex().associate { (index, path) -> path to index }
        val allMask = (1L shl allPaths.size) - 1
        var covered = 0L

        for (flat in flattenedPatterns) {
            when (flat) {
                FlattenedPattern.Wildcard -> covered = allMask
                is FlattenedPattern.Path -> {
                    val idx = indexByPath[flat.path]
                    if (idx != null) covered = covered or (1L shl idx)
                }
                is FlattenedPattern.Prefix -> {
                    for ((path, idx) in indexByPath) {
                        if (path.startsWith(flat.prefix)) covered = covered or (1L shl idx)
                    }
                }
            }
            if (covered == allMask) return ExhaustivenessResult.Exhaustive
        }

        return if (covered == allMask) {
            ExhaustivenessResult.Exhaustive
        } else {
            val missingPaths = allPaths.filterIndexed { idx, _ -> (covered and (1L shl idx)) == 0L }
            val missingPatterns = missingPaths.take(5).map { reconstructPattern(type, it) }
            ExhaustivenessResult.NonExhaustive(missingPatterns, source)
        }
    }

    private fun estimateFlattenedConstructors(
        type: ConeEnumType,
        context: CheckerContext,
        depth: Int,
    ): Int {
        if (depth >= maxNestingDepth) return 1
        val constructors = collectEnumConstructorNames(type, context)
        if (constructors.isEmpty()) return 1
        return constructors.size
    }

    private fun collectAllPaths(
        type: ConeEnumType,
        context: CheckerContext,
        depth: Int,
    ): List<String> {
        if (depth >= maxNestingDepth) return listOf("")
        val constructors = collectEnumConstructorNames(type, context)
        if (constructors.isEmpty()) return listOf("")
        return constructors
    }

    private fun flattenPattern(pattern: CfirMatchPattern): FlattenedPattern {
        return when (val kind = pattern.kind) {
            CfirMatchPatternKind.Wild, is CfirMatchPatternKind.Binding -> FlattenedPattern.Wildcard
            is CfirMatchPatternKind.Enum -> {
                val name = kind.entryName
                if (kind.subPatterns.isEmpty()) {
                    FlattenedPattern.Path(name)
                } else if (kind.subPatterns.all { it.kind == CfirMatchPatternKind.Wild || it.kind is CfirMatchPatternKind.Binding }) {
                    FlattenedPattern.Prefix("$name(")
                } else {
                    val subStrings = kind.subPatterns.map {
                        when (val flat = flattenPattern(it)) {
                            FlattenedPattern.Wildcard -> "_"
                            is FlattenedPattern.Path -> flat.path
                            is FlattenedPattern.Prefix -> "${flat.prefix}..."
                        }
                    }
                    FlattenedPattern.Path("$name(${subStrings.joinToString(",")})")
                }
            }
            else -> FlattenedPattern.Wildcard
        }
    }

    private fun reconstructPattern(type: ConeCangjieType, path: String): CfirMatchPattern {
        return CfirMatchPattern.wild(type)
    }

    companion object {
        val INSTANCE = NestedFlattenChecker()
    }
}

private sealed class FlattenedPattern {
    data object Wildcard : FlattenedPattern()
    data class Path(val path: String) : FlattenedPattern()
    data class Prefix(val prefix: String) : FlattenedPattern()
}

