package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.canHaveDeferredReturnTypeCalculation
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.scopes.CallableCopyTypeCalculator
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef

/**
 * BODY_RESOLVE 阶段的返回类型计算器。
 * 直接从声明的 `returnTypeRef` 中提取已解析类型。
 * 进入 BODY_RESOLVE 时，所有声明的返回类型都应已在 IMPLICIT_TYPES 阶段完成推断。
 * 参考 K2 `ReturnTypeCalculatorForFullBodyResolve`。
 */
class ReturnTypeCalculatorForFullBodyResolve : ReturnTypeCalculator() {
    override val callableCopyTypeCalculator: CallableCopyTypeCalculator.DeferredCallableCopyTypeCalculator
        get() = CallableCopyTypeCalculator.CalculateDeferredForceLazyResolution

    override fun tryCalculateReturnTypeOrNull(declaration: CfirCallableDeclaration): CfirResolvedTypeRef? {
        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is CfirResolvedTypeRef) {
            return returnTypeRef
        }

        if (declaration.canHaveDeferredReturnTypeCalculation) {
            return callableCopyTypeCalculator.computeReturnType(declaration)
        }

        return buildErrorTypeRef {
            diagnostic = ConeSimpleDiagnostic(
                "Cannot calculate return type during full-body resolution",
                DiagnosticKind.InferenceError,
            )
        }
    }

    companion object {
        val Default = ReturnTypeCalculatorForFullBodyResolve()
    }
}
