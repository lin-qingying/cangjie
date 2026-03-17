package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.enumMatchTracker
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.reportEnumUsageInMatch
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType

object CfirMatchExhaustivenessChecker : CfirMatchExpressionChecker(CheckerDispatchKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val subjectType = expression.subject.coneTypeOrNull ?: return
        val branches = expression.branches

        context.session.enumMatchTracker?.reportEnumUsageInMatch(context.containingFilePath, subjectType)

        if (hasWildcardOrBindingBranch(branches)) return

        val missingCases = computeMissingCases(subjectType, branches, context)
        if (missingCases.isNotEmpty()) {
            reporter.reportOn(source, CfirErrors.NON_EXHAUSTIVE_MATCH, missingCases)
        }
    }

    private fun hasWildcardOrBindingBranch(branches: List<CfirMatchBranch>): Boolean {
        return branches.any { branch ->
            branch.guard == null && isUnconditionalCatchAll(branch.pattern)
        }
    }

    private fun isUnconditionalCatchAll(pattern: CfirPattern): Boolean {
        return when (pattern) {
            is CfirWildcardPattern -> true
            is CfirBindingPattern -> pattern.nestedPattern == null || isUnconditionalCatchAll(pattern.nestedPattern!!)
            else -> false
        }
    }

    private fun computeMissingCases(
        subjectType: ConeCangjieType,
        branches: List<CfirMatchBranch>,
        context: CheckerContext,
    ): List<String> {
        return when {
            isBooleanType(subjectType) -> computeMissingBooleanCases(branches)
            subjectType is ConeEnumType -> computeMissingEnumCases(subjectType, branches, context)
            else -> listOf("_")
        }
    }

    private fun computeMissingBooleanCases(branches: List<CfirMatchBranch>): List<String> {
        var hasTrue = false
        var hasFalse = false

        for (branch in branches) {
            when (branch.pattern) {
                is CfirConstPattern -> {
                    val constType = (branch.pattern as CfirConstPattern).expression.coneTypeOrNull
                    if (constType != null && isBooleanType(constType)) {
                        // Conservative fallback: we only know one boolean literal is covered.
                        hasTrue = true
                    }
                }

                is CfirWildcardPattern, is CfirBindingPattern -> {
                    hasTrue = true
                    hasFalse = true
                }

                else -> Unit
            }
        }

        val missing = mutableListOf<String>()
        if (!hasTrue) missing += "true"
        if (!hasFalse) missing += "false"
        return missing
    }

    private fun computeMissingEnumCases(
        enumType: ConeEnumType,
        branches: List<CfirMatchBranch>,
        context: CheckerContext,
    ): List<String> {
        val coveredConstructors = mutableSetOf<String>()
        for (branch in branches) {
            collectCoveredEnumConstructors(branch.pattern, coveredConstructors)
        }

        val allConstructors = getEnumConstructorNames(enumType, context)
        if (allConstructors.isEmpty()) return emptyList()

        if ("*" in coveredConstructors) return emptyList()

        return allConstructors.filter { it !in coveredConstructors }
    }

    private fun collectCoveredEnumConstructors(pattern: CfirPattern, covered: MutableSet<String>) {
        when (pattern) {
            is CfirEnumPattern -> {
                val ref = pattern.constructorReference
                if (ref is CfirNamedReference) {
                    covered += ref.name.asString()
                }
            }

            is CfirWildcardPattern, is CfirBindingPattern -> covered += "*"
            else -> Unit
        }
    }

    private fun getEnumConstructorNames(enumType: ConeEnumType, context: CheckerContext): List<String> {
        val symbolProvider = context.session.symbolProvider
        val classSymbol = symbolProvider.getClassLikeSymbolByClassId(enumType.classId) ?: return emptyList()
        if (!classSymbol.isBound) return emptyList()
        val klass = classSymbol.cfir
        return klass.declarations
            .filterIsInstance<CfirEnumConstructor>()
            .map { it.name.asString() }
    }

    private fun isBooleanType(type: ConeCangjieType): Boolean {
        return type is ConePrimitiveType && type == ConePrimitiveType.BOOLEAN
    }
}
