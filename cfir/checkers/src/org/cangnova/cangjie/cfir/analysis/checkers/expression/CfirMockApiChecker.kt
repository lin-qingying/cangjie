package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.hasAnnotation
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name

/**
 * `createMock` / `createSpy` 语义边界检查。
 *
 * 当前 CFIR 先承接最稳定的一批规则：
 * - 目标类型必须是 class / interface；
 * - 被 mock 的冻结声明不允许；
 * - 泛型包装函数需要显式 `@Frozen`；
 * - 非测试路径默认拦截为 test-mode 约束；
 * - 其余未显式启用的路径统一落到 mock feature gate。
 */
object CfirMockApiChecker : CfirFunctionCallChecker() {
    private val createMockName = Name.identifier("createMock")
    private val createSpyName = Name.identifier("createSpy")
    private val frozenAnnotationName = Name.identifier("Frozen")
    private val preparedToMockAnnotationName = Name.identifier("EnsurePreparedToMock")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        if (expression.origin != CfirFunctionCallOrigin.MockIntrinsic) return

        val calleeName = (expression.calleeReference as? CfirNamedReference)?.name ?: return
        if (calleeName != createMockName && calleeName != createSpyName) return

        val source = expression.source ?: expression.calleeReference.source ?: return
        val targetType = expression.typeArguments.firstOrNull()?.coneTypeOrNull

        if (!targetType.isSupportedMockTarget()) {
            reporter.reportOn(source, CfirErrors.MOCK_UNSUPPORTED_TYPE)
            return
        }

        if (!context.containingFilePath.orEmpty().contains("test", ignoreCase = true)) {
            reporter.reportOn(source, CfirErrors.MOCK_NOT_IN_TEST_MODE, "--test")
            return
        }

        val targetClassId = targetType?.classIdOrPrimitiveClassId ?: return
        val targetDeclaration = context.session.cfirProvider.getCfirClassifierByFqName(targetClassId)
            ?: context.session.symbolProvider.getClassLikeSymbolByClassId(targetClassId)?.cfir as? CfirClassLikeDeclaration

        if (targetDeclaration != null && targetDeclaration.hasAnnotation(frozenAnnotationName)) {
            reporter.reportOn(source, CfirErrors.MOCK_FROZEN_UNSUPPORTED)
            return
        }

        val containingFunction = context.findClosestDeclaration<CfirFunction>()
        if (containingFunction != null && containingFunction.typeParameters.isNotEmpty() && !containingFunction.hasAnnotation(frozenAnnotationName)) {
            reporter.reportOn(source, CfirErrors.MOCK_FROZEN_REQUIRED, containingFunction.callableName())
            return
        }

        if (targetDeclaration == null) {
            reporter.reportOn(source, CfirErrors.MOCK_DISABLED, "--mock")
            return
        }

        if (!targetDeclaration.hasAnnotation(preparedToMockAnnotationName)) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.MOCK_DOESNT_SUPPORT_MOCKING,
                a = targetDeclaration.name,
                b = targetDeclaration.symbol.classId.packageFqName,
                c = "--mock-compatible",
            )
            return
        }

        reporter.reportOn(source, CfirErrors.MOCK_DISABLED, "--mock")
    }

    private fun CfirFunction.callableName(): Name {
        return (this as? org.cangnova.cangjie.cfir.declarations.CfirNamedFunction)?.name
            ?: Name.identifier("anonymous")
    }

    /**
     * 对齐官方 `only mocking of classes or interfaces is supported`：
     * 这里只看目标类型形态，不把“provider 暂时找不到本地声明”误判成 unsupported。
     */
    private fun ConeCangJieType?.isSupportedMockTarget(): Boolean = when (this) {
        null -> false
        is ConeFunctionType -> false
        is ConeTupleType -> false
        is ConeClassLikeType -> true
        is ConeTypeAliasType -> expandedType.isSupportedMockTarget()
        else -> false
    }
}
