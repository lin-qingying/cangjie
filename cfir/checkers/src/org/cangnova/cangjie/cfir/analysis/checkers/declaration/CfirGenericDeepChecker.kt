package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn

/**
 * 泛型深层检查器（GenericDeep 分组）
 *
 * 对齐 C++ TypeCheckGeneric.cpp:
 * - 泛型参数直接递归（上界引用自身）
 * - 泛型参数间接递归（上界通过与类无关的路径递归引用）
 *
 * 注意：参数个数匹配、约束宽松性、实例化歧义等检查依赖 resolve 阶段的完整信息，
 * 将在 resolve 管线就绪后补充。
 */
object CfirGenericDeepChecker : CfirTypeParameterChecker() {
    /**
     * 检查类型参数直接和间接递归上界。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeParameter) {
        val issue = with(context.session) {
            declaration.findFirstGenericUpperBoundRecursionIssueInOwner()
        } ?: return
        if (issue.parameter.symbol != declaration.symbol) return
        when (issue) {
            is CfirGenericUpperBoundRecursionIssue.Direct -> {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.GENERIC_PARAM_DIRECTLY_RECURSIVE,
                    a = declaration.name,
                    b = issue.recursiveWith.name,
                )
            }
            is CfirGenericUpperBoundRecursionIssue.ClassIrrelevant -> {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY,
                    a = declaration.name,
                    b = issue.upperBound,
                )
            }
        }
    }

}
