package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfAvailableExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * `@IfAvailable` 的特殊表达式 checker。
 *
 * 该节点已经在 parser/raw CFIR 中完成形状建模，因此 checker 只消费结构化
 * 条件、分支和文件 import，不再扫描普通 annotation 文本或重新解释 macro surface。
 */
object CfirIfAvailableExpressionChecker : CfirBasicExpressionChecker() {
    private val deviceInfoPackage = FqName("ohos.device_info")
    private val basePackage = FqName("ohos.base")

    /** 官方 IfAvailable 支持的条件参数名。 */
    private val supportedConditionNames = setOf("level", "syscap")

    /** 官方 IfAvailable 的最低 APILevel。 */
    private const val minimumLevel = 19

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        val ifAvailable = expression as? CfirIfAvailableExpression ?: return
        if (checkCondition(ifAvailable, reporter)) {
            checkImports(ifAvailable, reporter)
        }
        checkBranches(ifAvailable, reporter)
        checkJoinedBranchType(ifAvailable, reporter)
    }

    /** 检查命名条件和 literal 约束。 */
    context(context: CheckerContext)
    private fun checkCondition(
        expression: CfirIfAvailableExpression,
        reporter: DiagnosticReporter,
    ): Boolean {
        val conditionName = expression.conditionName.takeUnless { it.isSpecial }
        if (conditionName == null) {
            reporter.reportOn(
                source = expression.conditionArgumentSource ?: expression.source,
                factory = CfirErrors.IFAVAILABLE_ARG_NO_NAME,
            )
            return false
        }

        if (conditionName.asString() !in supportedConditionNames) {
            reporter.reportOn(
                source = expression.conditionNameSource ?: expression.conditionArgumentSource ?: expression.source,
                factory = CfirErrors.IFAVAILABLE_UNKNOWN_ARG_NAME,
                a = conditionName.asString(),
            )
            return false
        }

        val condition = expression.condition ?: return false
        if (condition !is CfirLiteralExpression) {
            reporter.reportOn(
                source = condition.source ?: expression.conditionArgumentSource ?: expression.source,
                factory = CfirErrors.IFAVAILABLE_ARG_NOT_LITERAL,
            )
            return false
        }

        if (conditionName.asString() == "level" && condition.value.toString().toIntOrNull()?.let { it < minimumLevel } == true) {
            reporter.reportOn(
                source = expression.source,
                factory = CfirErrors.IFAVAILABLE_LEVEL_LIMIT,
            )
        }
        return true
    }

    /**
     * 官方 `SynIfAvailableExpr` 要求 level/syscap 对应的运行时支持包已导入。
     * 这里读取已解析的 file import 元数据，包路径仍与条件身份分开保存。
     */
    context(context: CheckerContext)
    private fun checkImports(
        expression: CfirIfAvailableExpression,
        reporter: DiagnosticReporter,
    ) {
        val conditionName = expression.conditionName.asString()
        val requiredPackage = when (conditionName) {
            "level" -> deviceInfoPackage
            "syscap" -> basePackage
            else -> return
        }
        val file = context.containingFileSymbol?.cfir ?: return
        if (file.hasImportFor(requiredPackage)) return

        reporter.reportOn(
            source = expression.source,
            factory = CfirErrors.USE_EXPR_WITHOUT_IMPORT,
            a = requiredPackage,
            b = "IfAvailable",
        )
    }

    /** 合法分支必须是零参数 lambda；非 lambda 由该节点一次性报告。 */
    context(context: CheckerContext)
    private fun checkBranches(
        expression: CfirIfAvailableExpression,
        reporter: DiagnosticReporter,
    ) {
        listOf(expression.thenBranch, expression.elseBranch)
            .filterNot { it is CfirAnonymousFunctionExpression }
            .forEach { branch ->
                reporter.reportOn(
                    source = branch.source ?: expression.source,
                    factory = CfirErrors.IFAVAILABLE_ARG_NOT_LITERAL,
                )
        }
    }

    /**
     * IfAvailable 脱糖后是带 else 的 `if`；两个 lambda body 不能因为 lambda
     * wrapper 被单独建模而绕过官方的 branch join 检查。
     */
    context(context: CheckerContext)
    private fun checkJoinedBranchType(
        expression: CfirIfAvailableExpression,
        reporter: DiagnosticReporter,
    ) {
        val bodyTypes = listOf(expression.thenBranch, expression.elseBranch)
            .mapNotNull { (it as? CfirAnonymousFunctionExpression)?.anonymousFunction?.body?.coneTypeOrNull }
            .map { it.fullyExpandedType(context.session) }
        if (bodyTypes.size != 2 || bodyTypes.any { it is ConeErrorType }) return

        val first = bodyTypes[0]
        val second = bodyTypes[1]
        if (
            AbstractTypeChecker.isSubtypeOf(context.session.typeContext, first, second) == true ||
            AbstractTypeChecker.isSubtypeOf(context.session.typeContext, second, first) == true
        ) return

        reporter.reportOn(
            source = expression.source,
            factory = CfirErrors.TYPE_MISMATCH,
            a = first,
            b = second,
            c = false,
        )
    }

    /** 判断 import 是否直接导入目标包、其成员或 all-under。 */
    private fun CfirFile.hasImportFor(requiredPackage: FqName): Boolean = imports.any { import ->
        val imported = import.importedFqName ?: return@any false
        imported == requiredPackage || imported.parent() == requiredPackage
    }
}
