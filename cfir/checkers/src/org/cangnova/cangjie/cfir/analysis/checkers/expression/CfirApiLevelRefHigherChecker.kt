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

        for (declaration in availability.referenceAvailabilityChain(symbol)) {
            if (availability.hideUnavailabilityOf(declaration, useSitePackage) != null) {
                return AvailabilityFailure.Hide(reference.name.asString())
            }

            val apiLevel = availability.ownApiLevelInfo(declaration) ?: continue
            if (apiLevelProvider.projectApiLevel != CfirApiLevelProvider.DISABLED) {
                val targetLevel = apiLevel.since?.toIntOrNull()
                if (targetLevel != null && targetLevel > apiLevelProvider.projectApiLevel) {
                    return AvailabilityFailure.ApiLevel(
                        declarationName = declaration.platformDiagnosticName(reference.name),
                        targetLevel = targetLevel,
                        projectLevel = apiLevelProvider.projectApiLevel,
                    )
                }
            }

            val syscap = apiLevel.syscap
            if (!apiLevelProvider.syscapEnabled || syscap.isNullOrEmpty()) continue
            val diagnosticName = Name.identifier(syscap)
            if (syscap !in apiLevelProvider.syscapUnion) {
                return AvailabilityFailure.SyscapError(diagnosticName)
            }
            if (syscap !in apiLevelProvider.syscapIntersection) {
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
}
