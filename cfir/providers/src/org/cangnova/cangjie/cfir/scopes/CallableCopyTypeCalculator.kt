package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

/**
 * A utility class that, depending on the implementation, calculates the return type of callable copies,
 * (substitution/intersection overrides, delegated members, and enhanced Java declarations) and returns it.
 *
 * See [CfirDeclarationAttributes.deferredCallableCopyReturnType].
 */
abstract class CallableCopyTypeCalculator {
    /**
     * Returns the [CfirTypeRef] for [CfirCallableDeclaration.returnTypeRef] of the [declaration].
     *
     * Depending on the implementation, this call might invoke a deferred computation of the return type
     * (see [CfirDeclarationAttributes.deferredCallableCopyReturnType]).
     *
     * A return value of `null` signifies that the calculation has failed or that no deferred computation was stored
     * and the return type could not be resolved ordinarily.
     */
    abstract fun computeReturnType(declaration: CfirCallableDeclaration): CfirTypeRef?

    fun computeReturnTypeOrNull(declaration: CfirCallableDeclaration): ConeCangJieType? {
        return computeReturnType(declaration)?.coneTypeOrNull
    }

    /**
     * Doesn't perform any calculation and returns [CfirCallableDeclaration.returnTypeRef].
     */
    object DoNothing : CallableCopyTypeCalculator() {
        override fun computeReturnType(declaration: CfirCallableDeclaration): CfirTypeRef {
            return declaration.returnTypeRef
        }
    }

    /**
     * If necessary, runs the computation saved in [CfirDeclarationAttributes.deferredCallableCopyReturnType] and returns a [CfirResolvedTypeRef].
     */
    abstract class DeferredCallableCopyTypeCalculator : CallableCopyTypeCalculator() {
        override fun computeReturnType(declaration: CfirCallableDeclaration): CfirResolvedTypeRef? {
            val callableCopyDeferredTypeCalculation = declaration.attributes.deferredCallableCopyReturnType
                ?: return declaration.getResolvedTypeRef()

            synchronized(callableCopyDeferredTypeCalculation) {
                if (declaration.attributes.deferredCallableCopyReturnType == null) {
                    return declaration.returnTypeRef as CfirResolvedTypeRef
                }

                val returnType = callableCopyDeferredTypeCalculation.computeReturnType(this) ?: return null
                val returnTypeRef = declaration.returnTypeRef.resolvedTypeFromPrototype(
                    returnType, declaration.source?.fakeElement(CjFakeSourceElementKind.ImplicitTypeRef)
                )

                declaration.replaceReturnTypeRef(returnTypeRef)
                if (declaration is CfirProperty) {
                    declaration.getter?.replaceReturnTypeRef(returnTypeRef)
                    declaration.setter?.valueParameters?.firstOrNull()?.replaceReturnTypeRef(returnTypeRef)
                }

                declaration.attributes.deferredCallableCopyReturnType = null

                return returnTypeRef
            }
        }

        protected abstract fun CfirCallableDeclaration.getResolvedTypeRef(): CfirResolvedTypeRef?
    }

    /**
     * Run deferred return types calculations and forces lazy resolution of overridden declarations.
     *
     * @see DeferredCallableCopyTypeCalculator
     * @see CfirDeclarationAttributes.deferredCallableCopyReturnType
     */
    object CalculateDeferredForceLazyResolution : DeferredCallableCopyTypeCalculator() {
        override fun CfirCallableDeclaration.getResolvedTypeRef(): CfirResolvedTypeRef? {
            return symbol.resolvedReturnTypeRef
        }
    }

    /**
     * Run deferred return types calculations but doesn't force lazy resolution of overridden declarations.
     *
     * @see DeferredCallableCopyTypeCalculator
     * @see CfirDeclarationAttributes.deferredCallableCopyReturnType
     */
    object CalculateDeferredWhenPossible : DeferredCallableCopyTypeCalculator() {
        override fun CfirCallableDeclaration.getResolvedTypeRef(): CfirResolvedTypeRef? {
            return returnTypeRef as? CfirResolvedTypeRef
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------------

private object DeferredCallableCopyReturnTypeKey : CfirDeclarationDataKey()

var CfirDeclarationAttributes.deferredCallableCopyReturnType: DeferredCallableCopyReturnType? by CfirDeclarationDataRegistry.attributesAccessor(
    DeferredCallableCopyReturnTypeKey
)

abstract class DeferredCallableCopyReturnType {
    /**
     * Performs a deferred computation some declaration's return type.
     *
     * [calc] must be used for the return type calculation of overridden members which might recursively trigger the computation of
     * deferred return types.
     */
    abstract fun computeReturnType(calc: CallableCopyTypeCalculator): ConeCangJieType?
}

