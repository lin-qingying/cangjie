package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.containsReportedErrorDiagnostic
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.resolve.match.CfirTuplePatternShape
import org.cangnova.cangjie.cfir.resolve.match.resolveTupleShape
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * 元组变量声明的整体形状与逐分量检查。
 *
 * 官方 SynchronizeTypeAndInitializer 在根形状失败时直接报告专用错误；根形状成立后，
 * ChkPattern 的嵌套失败还会在完整声明模式上报告 MISMATCHED_TYPE_FOR_PATTERN_IN_VARDECL。
 */
object CfirTuplePatternDeclarationChecker : CfirPatternVariableChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirPatternVariable) {
        val pattern = declaration.pattern as? CfirTuplePattern ?: return
        if (declaration.initializer?.coneTypeOrNull is ConeErrorType) return
        if (declaration.initializer?.containsReportedErrorDiagnostic() == true) return
        val type = declaration.returnTypeRef.coneTypeOrNull ?: return
        checkShape(pattern, type, isRoot = true)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkShape(pattern: CfirTuplePattern, type: ConeCangJieType, isRoot: Boolean): Boolean {
        when (val shape = pattern.resolveTupleShape(type, context.session)) {
            CfirTuplePatternShape.Unresolved -> return false
            is CfirTuplePatternShape.NotTuple -> {
                if (isRoot) {
                    reporter.reportOn(pattern.source, CfirErrors.TUPLE_PATTERN_NOT_MATCH, "initializer")
                } else {
                    reporter.reportOn(pattern.source, CfirErrors.TYPE_MISMATCH, type, ConeTupleType(emptyList()), false)
                }
                return true
            }
            is CfirTuplePatternShape.WrongSize -> {
                reporter.reportOn(pattern.source, CfirErrors.TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED)
                return true
            }
            is CfirTuplePatternShape.Matched -> {
                for ((index, element) in pattern.elements.withIndex()) {
                    val nested = element as? CfirTuplePattern ?: continue
                    if (checkShape(nested, shape.tupleType.elementTypes[index], isRoot = false)) {
                        if (isRoot) reporter.reportOn(pattern.source, CfirErrors.MISMATCHED_TYPE_FOR_PATTERN_IN_VARDECL)
                        return true
                    }
                }
            }
        }
        return false
    }
}
