package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.canHaveDeferredReturnTypeCalculation
import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.isCopyCreatedInScope
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.scopes.CallableCopyTypeCalculator
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 带 designated resolve“跳转”能力的返回类型计算器。
 *
 * 对齐 Kotlin K2 `ReturnTypeCalculatorWithJump`：普通隐式返回类型通过 designated body resolve
 * 按需推进；callable copy 的延迟返回类型通过 [CallableCopyTypeCalculator] 委托回同一条计算路径。
 */
open class ReturnTypeCalculatorWithJump(
    protected val session: CfirSession,
    protected val scopeSession: ScopeSession,
    protected val implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
) : ReturnTypeCalculator() {
    override val callableCopyTypeCalculator: CallableCopyTypeCalculator = CallableCopyTypeCalculatorWithJump()

    override fun tryCalculateReturnTypeOrNull(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        if (declaration is CfirPatternBindingVariable) {
            calculatePatternBindingReturnTypeOrNull(declaration)?.let { return it }
        }

        if (declaration.isLocal) {
            return ReturnTypeCalculatorForFullBodyResolve.Default.tryCalculateReturnType(declaration)
        }

        if (declaration is CfirValueParameter && declaration.returnTypeRef is CfirImplicitTypeRef) {
            declaration.replaceReturnTypeRef(
                buildErrorTypeRef {
                    diagnostic = ConeSimpleDiagnostic(
                        "Unsupported: implicit value parameter type",
                        DiagnosticKind.InferenceError,
                    )
                }
            )
        }

        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is CfirResolvedTypeRef) return returnTypeRef

        if (declaration.canHaveDeferredReturnTypeCalculation) {
            val resolvedTypeRef = callableCopyTypeCalculator.computeReturnType(declaration)
            requireWithAttachment(
                resolvedTypeRef is CfirResolvedTypeRef,
                { "Unexpected return type: ${resolvedTypeRef?.let { it::class.simpleName }}" },
            ) {
                withCfirEntry("declaration", declaration)
            }

            return resolvedTypeRef
        }

        return computeReturnTypeRef(declaration)
    }

    protected fun recursionInImplicitTypeRef(declaration: CfirCallableDeclaration): CfirResolvedTypeRef =
        buildErrorTypeRef {
            diagnostic = ConeSimpleDiagnostic("Recursive implicit type", DiagnosticKind.RecursionInImplicitTypes)
        }.also {
            implicitBodyResolveComputationSession.calculateAndStoreNonTrivialLoop(declaration.symbol)
        }

    private fun computeReturnTypeRef(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        val symbol = declaration.symbol
        val computedReturnType = when (val status = implicitBodyResolveComputationSession.getStatus(symbol)) {
            is CfirImplicitBodyResolveComputationStatus.Computed -> status.resolvedTypeRef
            is CfirImplicitBodyResolveComputationStatus.Computing -> recursionInImplicitTypeRef(declaration)
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> null
        }

        (computedReturnType ?: declaration.returnTypeRef as? CfirResolvedTypeRef)?.let { return it }
        require(!declaration.isCopyCreatedInScope) {
            "callableCopySubstitution was not calculated for callable copy: " +
                    "$symbol with origin ${declaration.origin} and return type ${declaration.returnTypeRef}"
        }

        resolveDeclaration(declaration)
        return declaration.returnTypeRef as? CfirResolvedTypeRef
            ?: errorWithAttachment("${this::class.simpleName}: Return type cannot be calculated for ${declaration::class.simpleName}") {
                withCfirEntry("declaration", declaration)
            }
    }

    protected open fun resolveDeclaration(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        val file = session.cfirProvider.getContainingFile(declaration.symbol)
        val containingClassLookupTag = declaration.symbol.containingClassLookupTag()
        val outerClasses = generateSequence(containingClassLookupTag) { lookupTag ->
            lookupTag.toSymbol(session)?.getContainingClassSymbol()?.toLookupTag()
        }.mapTo(mutableListOf()) { lookupTag ->
            lookupTag.toSymbol(session)?.cfir as? CfirClassLikeDeclaration
        }

        if (file == null || outerClasses.any { it == null }) {
            return buildErrorTypeRef {
                diagnostic = ConeSimpleDiagnostic(
                    "Cannot calculate return type (local class/object?)",
                    DiagnosticKind.InferenceError,
                )
            }
        }

        val designation = listOf(file) + outerClasses.filterNotNull().asReversed()
        val transformer = CfirDesignatedBodyResolveTransformerForReturnTypeCalculator(
            designation = (designation.drop(1) + declaration).iterator(),
            session = session,
            scopeSession = scopeSession,
            implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
            returnTypeCalculator = this,
        )

        designation.first().transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextDependent)

        val transformedDeclaration = transformer.lastResult as? CfirCallableDeclaration
            ?: error("Unexpected lastResult: ${transformer.lastResult}")

        val newReturnTypeRef = transformedDeclaration.returnTypeRef
        require(newReturnTypeRef is CfirResolvedTypeRef) { transformedDeclaration }
        return newReturnTypeRef
    }

    /**
     * Pattern binding 的名字解析 symbol 与隐式类型推断 owner 不同：
     * binding 自身没有 initializer，类型由外层 pattern variable 推断后投影写回。
     */
    private fun calculatePatternBindingReturnTypeOrNull(
        declaration: CfirPatternBindingVariable,
    ): CfirResolvedTypeRef? {
        (declaration.returnTypeRef as? CfirResolvedTypeRef)?.let { return it }

        val owner = session.cfirProvider.getCfirPatternVariableForBinding(declaration.symbol) ?: return null
        if (implicitBodyResolveComputationSession.getStatus(owner.symbol) is CfirImplicitBodyResolveComputationStatus.Computing) {
            return recursionInImplicitTypeRef(declaration)
        }

        tryCalculateReturnTypeOrNull(owner)
        return declaration.returnTypeRef as? CfirResolvedTypeRef
    }

    private inner class CallableCopyTypeCalculatorWithJump : CallableCopyTypeCalculator.DeferredCallableCopyTypeCalculator() {
        override fun CfirCallableDeclaration.getResolvedTypeRef(): CfirResolvedTypeRef {
            return this@ReturnTypeCalculatorWithJump.computeReturnTypeRef(this)
        }
    }
}
