package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.declarationAvailabilityProvider
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirIfAvailableBranchKind
import org.cangnova.cangjie.cfir.declarations.ifAvailableBranchContext
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.CfirApiLevelProvider
import org.cangnova.cangjie.cfir.session.apiLevelProvider
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement

/**
 * 已解析声明的 OHOS 平台可用性检查器。
 *
 * 名称解析、重载选择和冲突检测完成后，这里才对最终
 * [CfirResolvedNamedReference] 执行官方顺序：
 * `outer Hide → outer APILevel → outer Syscap → target Hide → target APILevel → target Syscap`。
 * 任一步失败都短路后续检查，但不改写已完成的符号绑定和表达式类型。
 */
object CfirApiLevelRefHigherChecker : CfirQualifiedAccessChecker() {
    /** 按最终 resolved target 检查 Hide、APILevel 与 Syscap。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        val failure = availabilityFailure(expression) ?: return

        // collector 采用 preorder；检查内层 receiver 时，栈中已经包含当前节点及所有外层访问。
        // 官方 walker 在外层 CheckNode 首次失败后 SKIP_CHILDREN，因此外层已有失败时不再报告内层。
        val outerHasAvailabilityFailure = context.callsOrAssignments
            .asReversed()
            .drop(1)
            .filterIsInstance<CfirQualifiedAccessExpression>()
            .any { outer -> availabilityFailure(outer) != null }
        if (outerHasAvailabilityFailure) return

        val expressionSource = expression.source ?: return
        val diagnosticSource = CjOffsetsOnlySourceElement(
            expressionSource.startOffset,
            expressionSource.endOffset,
        )
        when (failure) {
            is AvailabilityFailure.Hide -> reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.UNRESOLVED_REFERENCE,
                a = failure.referencedName,
                b = null,
            )

            is AvailabilityFailure.ApiLevel -> reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.APILEVEL_REF_HIGHER,
                a = failure.declarationName,
                b = failure.targetLevel,
                c = failure.projectLevel,
            )

            is AvailabilityFailure.SyscapError -> reporter.reportOn(
                diagnosticSource,
                CfirErrors.APILEVEL_SYSCAP_ERROR,
                failure.syscap,
            )

            is AvailabilityFailure.SyscapWarning -> reporter.reportOn(
                diagnosticSource,
                CfirErrors.APILEVEL_SYSCAP_WARNING,
                failure.syscap,
            )
        }
    }

    /**
     * 无副作用查询最终 resolved access 的首个可用性失败。
     *
     * 该入口同时供当前节点报告与外层节点短路判断使用，确保两条路径消费完全一致的
     * outer/target 顺序和项目配置，不通过诊断 reporter 隐式改变遍历结果。
     */
    context(context: CheckerContext)
    private fun availabilityFailure(expression: CfirQualifiedAccessExpression): AvailabilityFailure? {
        val reference = expression.calleeReference as? CfirResolvedNamedReference ?: return null
        val symbol = reference.resolvedSymbol.takeIf { it.isBound } ?: return null
        val file = context.containingFileSymbol?.takeIf { it.isBound }?.cfir ?: return null
        val useSitePackage = file.packageDirective.packageFqName
        val availability = context.session.declarationAvailabilityProvider
        val apiLevelProvider = context.session.apiLevelProvider
        val scope = context.ifAvailableScope(expression)
        val declarationApiLevel = context.containingDeclarations
            .asSequence()
            .mapNotNull { containingSymbol ->
                availability.ownApiLevelInfo(containingSymbol.cfir)?.since?.toIntOrNull()
            }
            .minOrNull()

        for (declaration in availability.referenceAvailabilityChain(symbol)) {
            if (availability.hideUnavailabilityOf(declaration, useSitePackage) != null) {
                return AvailabilityFailure.Hide(reference.name.asString())
            }

            val apiLevel = availability.ownApiLevelInfo(declaration) ?: continue
            val currentApiLevel = scope.apiLevel ?: declarationApiLevel ?: apiLevelProvider.projectApiLevel
            if (currentApiLevel != CfirApiLevelProvider.DISABLED) {
                val targetLevel = apiLevel.since?.toIntOrNull()
                if (targetLevel != null && targetLevel > currentApiLevel) {
                    return AvailabilityFailure.ApiLevel(
                        declarationName = declaration.platformDiagnosticName(reference.name),
                        targetLevel = targetLevel,
                        projectLevel = currentApiLevel,
                    )
                }
            }

            val syscap = apiLevel.syscap
            if (!apiLevelProvider.syscapEnabled || syscap.isNullOrEmpty()) continue
            val diagnosticName = Name.identifier(syscap)
            if (syscap !in scope.syscapUnion(apiLevelProvider)) {
                return AvailabilityFailure.SyscapError(diagnosticName)
            }
            if (syscap !in scope.syscapIntersection(apiLevelProvider)) {
                return AvailabilityFailure.SyscapWarning(diagnosticName)
            }
        }
        return null
    }

    /** 为 outer 声明选择稳定诊断名；无名 extend 回用引用目标名。 */
    private fun CfirDeclaration.platformDiagnosticName(defaultName: Name): Name = when (this) {
        is CfirNamedFunction -> name
        is CfirProperty -> name
        is CfirClassLikeDeclaration -> symbol.classId.shortClassName
        else -> defaultName
    }

    /** 可用性查询返回的结构化首失败，不携带 reporter 或 source 副作用。 */
    private sealed interface AvailabilityFailure {
        /** Hide 使最终引用在当前包不可用。 */
        data class Hide(val referencedName: String) : AvailabilityFailure

        /** 声明要求的 APILevel 高于项目级别。 */
        data class ApiLevel(
            val declarationName: Name,
            val targetLevel: Int,
            val projectLevel: Int,
        ) : AvailabilityFailure

        /** Syscap 不在项目 union 中。 */
        data class SyscapError(val syscap: Name) : AvailabilityFailure

        /** Syscap 在 union 中但不在 intersection 中。 */
        data class SyscapWarning(val syscap: Name) : AvailabilityFailure
    }

    /**
     * `@IfAvailable` 分支内的有效 API/syscap 假设。
     *
     * true 分支把条件提升为当前分支已知事实；false 分支从全局候选集合中排除
     * 该事实。嵌套节点按 containing-elements 的外到内顺序叠加，保证同一规则
     * 同时适用于 PSI 和 LightTree lowering。
     */
    private data class IfAvailableScope(
        val apiLevel: Int? = null,
        val addedSyscaps: Set<String> = emptySet(),
        val removedSyscaps: Set<String> = emptySet(),
        val addedIntersectionSyscaps: Set<String> = emptySet(),
        val removedIntersectionSyscaps: Set<String> = emptySet(),
    ) {
        fun syscapUnion(provider: CfirApiLevelProvider): Set<String> =
            (provider.syscapUnion + addedSyscaps) - removedSyscaps

        fun syscapIntersection(provider: CfirApiLevelProvider): Set<String> =
            (provider.syscapIntersection + addedIntersectionSyscaps) - removedIntersectionSyscaps
    }

    /** 从 checker traversal 的结构栈计算当前引用所在的 IfAvailable 分支假设。 */
    context(context: CheckerContext)
    private fun CheckerContext.ifAvailableScope(reference: CfirQualifiedAccessExpression): IfAvailableScope {
        var scope = IfAvailableScope()
        for (element in containingElements) {
            val branch = (element as? CfirAnonymousFunction)?.ifAvailableBranchContext ?: continue
            when (branch.conditionName) {
                "level" -> {
                    val level = branch.conditionValue.toIntOrNull() ?: continue
                    scope = when (branch.kind) {
                        CfirIfAvailableBranchKind.THEN -> {
                            // 官方嵌套 IfAvailable 的 true 分支按当前条件重新建立
                            // APILevel 上界；false 分支在没有外层上界时取当前
                            // 项目/声明 scope 与当前条件的较小值。
                            scope.copy(apiLevel = level)
                        }

                        CfirIfAvailableBranchKind.ELSE -> {
                            // 官方 CheckIfAvailableExpr 的 else walker 继续使用
                            // 进入当前 IfAvailable 前的 scopeAPILevel；false 分支
                            // 不把条件值错误地降成新的 API 上界。否则外层 false
                            // 分支中的嵌套 IfAvailable 会把全局 level 23 错降为
                            // 外层条件 20，进而误报 f21/f22/f23。
                            scope
                        }
                    }
                }

                "syscap" -> {
                    val syscap = branch.conditionValue.removeSurrounding("\"")
                    scope = if (branch.kind == CfirIfAvailableBranchKind.THEN) {
                        scope.copy(
                            addedSyscaps = scope.addedSyscaps + syscap,
                            addedIntersectionSyscaps = scope.addedIntersectionSyscaps + syscap,
                            removedSyscaps = scope.removedSyscaps - syscap,
                            removedIntersectionSyscaps = scope.removedIntersectionSyscaps - syscap,
                        )
                    } else {
                        // false 分支只排除当前条件在全局并集中不存在的能力；全局
                        // 已知的 capability 仍可能在其他设备上存在，并按 intersection
                        // 规则继续产生 warning。
                        val provider = context.session.apiLevelProvider
                        val mustRemoveFromUnion =
                            syscap !in provider.syscapUnion && syscap !in scope.addedSyscaps
                        scope.copy(
                            removedSyscaps = if (mustRemoveFromUnion) scope.removedSyscaps + syscap else scope.removedSyscaps,
                            removedIntersectionSyscaps = scope.removedIntersectionSyscaps,
                            addedSyscaps = if (mustRemoveFromUnion) scope.addedSyscaps - syscap else scope.addedSyscaps,
                            addedIntersectionSyscaps = scope.addedIntersectionSyscaps,
                        )
                    }
                }
            }
        }
        return scope
    }
}
