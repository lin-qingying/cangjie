package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.calculateMatrix
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria.MarangetChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria.Usefulness
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.name.ClassId

/**
 * match 分支可达性检查器。
 *
 * 对齐 C++ DiagKind::sema_unreachable_pattern:
 * 如果某一分支的模式被之前无 guard 分支完全覆盖，则该分支不可达。
 * 覆盖关系复用共享 Maranget usefulness 模型，避免 wildcard、enum、tuple、type pattern
 * 在可达性和穷尽性两套算法中产生不同语义。
 */
object CfirMatchUnreachablePatternChecker : CfirMatchExpressionChecker() {
    /**
     * 检查 selector-based match 分支是否被前序无 guard 分支完全覆盖。
     *
     * pattern 合法性已经失败时跳过，避免在错误 pattern 上继续运行 usefulness 算法。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val subjectType = expression.subject?.coneTypeOrNull ?: return
        if (subjectType is ConeErrorType) return
        if (expression.hasPatternLegalityProblem(context)) return

        val matchContext = MatchExhaustivenessContext.fromSession(context.session)
        val knownConstructor = expression.subject?.knownEnumConstructorOrNull(subjectType, context)
        val previousRows = mutableListOf<List<CfirMatchPattern>>()

        for (branch in expression.branches) {
            val branchRows = runCatching {
                branch.pattern.calculateMatrix(subjectType, context.session)
            }.getOrElse { emptyList() }

            if (branchRows.isNotEmpty() && branchRows.all { row ->
                    row.isUnreachable(previousRows, matchContext, knownConstructor)
                }) {
                reporter.reportOn(
                    source = branch.pattern.source ?: branch.source,
                    factory = CfirErrors.UNREACHABLE_PATTERN,
                )
            }

            if (branch.guard == null) {
                previousRows += branchRows
            }
        }
    }

    /**
     * 判断一行 pattern 是否不可达。
     *
     * 若 subject 已知为某个 enum 构造器且当前行不可能匹配它，直接不可达；否则委托覆盖算法判断。
     */
    private fun List<CfirMatchPattern>.isUnreachable(
        previousRows: CfirMatrix,
        context: MatchExhaustivenessContext,
        knownConstructor: KnownEnumConstructor?,
    ): Boolean {
        if (knownConstructor != null && none { it.mayMatch(knownConstructor) }) return true
        return isCoveredBy(previousRows, context)
    }

    /**
     * 判断单个 pattern 是否可能匹配已知 enum 构造器。
     */
    private fun CfirMatchPattern.mayMatch(knownConstructor: KnownEnumConstructor): Boolean =
        when (val patternKind = kind) {
            is CfirMatchPatternKind.Enum ->
                patternKind.enumClassId == knownConstructor.enumClassId &&
                    patternKind.entryName == knownConstructor.entryName

            CfirMatchPatternKind.Error,
            CfirMatchPatternKind.Wild,
            is CfirMatchPatternKind.Binding,
            is CfirMatchPatternKind.Const,
            is CfirMatchPatternKind.Tuple,
            is CfirMatchPatternKind.Type,
            -> true
        }

    /**
     * 尝试从 match subject 的静态形态推导已知 enum 构造器。
     *
     * 直接构造器访问和由局部变量初始化转发的构造器访问都会被识别。
     */
    private fun CfirExpression.knownEnumConstructorOrNull(
        subjectType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        context: CheckerContext,
    ): KnownEnumConstructor? {
        val enumType = subjectType.expandedPatternEnumType(context.session) ?: return null
        val entryName = knownEnumConstructorEntryNameOrNull() ?: return null
        return KnownEnumConstructor(enumType.classId, entryName)
    }

    /**
     * 提取表达式已知 enum 构造器的 entry 名称。
     */
    private fun CfirExpression.knownEnumConstructorEntryNameOrNull(): String? {
        unwrapSingleExpressionBlock().takeIf { it !== this }?.knownEnumConstructorEntryNameOrNull()?.let { return it }
        directEnumConstructorEntryNameOrNull()?.let { return it }

        val variable = (this as? CfirQualifiedAccessExpression)
            ?.takeIf { it.explicitReceiver == null && it.dispatchReceiver == null }
            ?.resolvedVariableOrNull()
            ?: return null

        return variable.initializer?.unwrapSingleExpressionBlock()?.directEnumConstructorEntryNameOrNull()
    }

    /**
     * 从直接 enum constructor 调用或命名访问中提取 entry 名称。
     */
    private fun CfirExpression.directEnumConstructorEntryNameOrNull(): String? {
        val access = when (this) {
            is CfirFunctionCall -> this
            is CfirNamedAccessExpression -> this
            is CfirQualifiedAccessExpression -> this
            else -> return null
        }
        val enumConstructor = access.calleeReference.resolvedSymbolOrNull()
            ?.takeIf { it.isBound }
            ?.cfir as? CfirEnumConstructor
            ?: return null
        return enumConstructor.name.asString()
    }

    /**
     * 去掉只包含单个表达式的 block 包裹。
     */
    private fun CfirExpression.unwrapSingleExpressionBlock(): CfirExpression =
        (this as? CfirBlock)?.statements?.singleOrNull() as? CfirExpression ?: this

    /**
     * 从 qualified access 中解析变量声明。
     */
    private fun CfirQualifiedAccessExpression.resolvedVariableOrNull(): CfirVariable? =
        (calleeReference.resolvedSymbolOrNull() as? CfirVariableSymbol<*>)
            ?.takeIf { it.isBound }
            ?.cfir

    /**
     * 从引用节点提取已解析或候选符号。
     */
    private fun org.cangnova.cangjie.cfir.references.CfirReference.resolvedSymbolOrNull(): CfirBasedSymbol<*>? =
        when (this) {
            is CfirResolvedNamedReference -> resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> candidateSymbol
            else -> null
        }

    /**
     * 已知 enum 构造器的最小表示。
     *
     * @property enumClassId enum 类的 ClassId。
     * @property entryName enum entry 的名称文本。
     */
    private data class KnownEnumConstructor(
        val enumClassId: ClassId,
        val entryName: String,
    )

    /**
     * 使用 Maranget usefulness 算法判断当前 pattern 行是否已被前序矩阵覆盖。
     */
    private fun List<CfirMatchPattern>.isCoveredBy(
        previousRows: CfirMatrix,
        context: MatchExhaustivenessContext,
    ): Boolean {
        return MarangetChecker.INSTANCE.isUseful(
            matrix = previousRows,
            patterns = this,
            withWitness = false,
            context = context,
            isTopLevel = true,
        ) is Usefulness.Useless
    }
}
