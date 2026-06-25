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
    /**
     * full body resolve 阶段用于强制推进延迟 callable copy 返回类型的计算器。
     */
    override val callableCopyTypeCalculator: CallableCopyTypeCalculator.DeferredCallableCopyTypeCalculator
        get() = CallableCopyTypeCalculator.CalculateDeferredForceLazyResolution

    /**
     * 从已解析 return type ref 读取返回类型，或对允许延迟计算的 callable 触发 copy 返回类型计算。
     */
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
        /**
         * full-body resolve 返回类型计算器的共享默认实例。
         */
        val Default = ReturnTypeCalculatorForFullBodyResolve()
    }
}
