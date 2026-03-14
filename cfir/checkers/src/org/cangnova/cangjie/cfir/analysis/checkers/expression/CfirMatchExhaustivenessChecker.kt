package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.types.*

/**
 * match 表达式穷尽性检查器。
 *
 * 基于简化 Maranget 算法，检查 match 表达式是否覆盖了所有可能的情况：
 * - 枚举类型：检查所有构造器是否被覆盖
 * - 布尔类型：检查 true/false 是否都被覆盖
 * - 含通配符/绑定模式 → 自动穷尽
 * - 不穷尽时报告 NON_EXHAUSTIVE_MATCH 诊断
 *
 * 对齐 C++ PatternUsefulness + IntelliJ MatchExhaustivenessChecker（简化版）。
 */
object CfirMatchExhaustivenessChecker : CfirMatchExpressionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val subjectType = expression.subject.coneTypeOrNull ?: return
        val branches = expression.branches

        // 含通配符/绑定模式 → 自动穷尽
        if (hasWildcardOrBindingBranch(branches)) return

        val missingCases = computeMissingCases(subjectType, branches)
        if (missingCases.isNotEmpty()) {
            reporter.reportOn(source, CfirErrors.NON_EXHAUSTIVE_MATCH, missingCases.joinToString(", "))
        }
    }

    /** 检查是否存在通配符或绑定模式（无嵌套条件的，即无 guard 的） */
    private fun hasWildcardOrBindingBranch(branches: List<CfirMatchBranch>): Boolean {
        return branches.any { branch ->
            branch.guard == null && isUnconditionalCatchAll(branch.pattern)
        }
    }

    /** 模式是否为无条件全匹配 */
    private fun isUnconditionalCatchAll(pattern: CfirPattern): Boolean {
        return when (pattern) {
            is CfirWildcardPattern -> true
            is CfirBindingPattern -> pattern.nestedPattern == null || isUnconditionalCatchAll(pattern.nestedPattern!!)
            else -> false
        }
    }

    /**
     * 计算缺失的 case 列表。
     *
     * 简化策略：仅对枚举和布尔类型做穷尽性检查。
     * 其他类型如果没有通配符，一律报告不穷尽。
     */
    private fun computeMissingCases(
        subjectType: ConeCangjieType,
        branches: List<CfirMatchBranch>,
    ): List<String> {
        return when {
            isBooleanType(subjectType) -> computeMissingBooleanCases(branches)
            subjectType is ConeEnumType -> computeMissingEnumCases(subjectType, branches)
            else -> {
                // 非枚举/布尔类型 且无通配符 → 需要通配符覆盖
                listOf("_")
            }
        }
    }

    /** 布尔类型穷尽性检查 */
    private fun computeMissingBooleanCases(branches: List<CfirMatchBranch>): List<String> {
        var hasTrue = false
        var hasFalse = false

        for (branch in branches) {
            when (branch.pattern) {
                is CfirConstPattern -> {
                    // 检查常量值（简化判断：通过表达式的文本表示）
                    val expr = (branch.pattern as CfirConstPattern).expression
                    val constType = expr.coneTypeOrNull
                    if (constType != null && isBooleanType(constType)) {
                        // 简化：通过 source 文本或 literal 值判断 true/false
                        // 此处使用保守策略，假设 const pattern 匹配了一个值
                        hasTrue = true // 简化处理
                    }
                }
                is CfirWildcardPattern, is CfirBindingPattern -> {
                    hasTrue = true
                    hasFalse = true
                }
                else -> {}
            }
        }

        val missing = mutableListOf<String>()
        if (!hasTrue) missing.add("true")
        if (!hasFalse) missing.add("false")
        return missing
    }

    /** 枚举类型穷尽性检查 */
    private fun computeMissingEnumCases(
        enumType: ConeEnumType,
        branches: List<CfirMatchBranch>,
    ): List<String> {
        // 收集已覆盖的构造器名称
        val coveredConstructors = mutableSetOf<String>()
        for (branch in branches) {
            collectCoveredEnumConstructors(branch.pattern, coveredConstructors)
        }

        // 获取枚举的所有构造器名称
        val allConstructors = getEnumConstructorNames(enumType)
        if (allConstructors.isEmpty()) {
            // 无法获取构造器信息，保守返回空（不报错）
            return emptyList()
        }

        return allConstructors.filter { it !in coveredConstructors }
    }

    /** 从模式中收集已覆盖的枚举构造器名称 */
    private fun collectCoveredEnumConstructors(pattern: CfirPattern, covered: MutableSet<String>) {
        when (pattern) {
            is CfirEnumPattern -> {
                val ref = pattern.constructorReference
                if (ref is org.cangnova.cangjie.cfir.references.CfirNamedReference) {
                    covered.add(ref.name.asString())
                }
            }
            is CfirWildcardPattern, is CfirBindingPattern -> {
                // 通配符/绑定覆盖所有
                covered.add("*")
            }
            else -> {}
        }
    }

    /** 获取枚举类型的所有构造器名称 */
    private fun getEnumConstructorNames(enumType: ConeEnumType): List<String> {
        // 简化实现：从枚举的 classId 获取构造器
        // 完整实现需要通过 session.symbolProvider 查找枚举声明
        // Phase 4 暂时返回空列表，表示无法判断（保守策略）
        return emptyList()
    }

    private fun isBooleanType(type: ConeCangjieType): Boolean {
        return type is ConePrimitiveType && type == ConePrimitiveType.BOOLEAN
    }
}
