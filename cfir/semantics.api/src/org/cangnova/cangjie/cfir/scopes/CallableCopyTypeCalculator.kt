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
 * callable copy 返回类型计算器。
 *
 * 不同实现可用于 substitution override、intersection override、委托成员和增强声明等场景，
 * 返回值通过 [CfirDeclarationAttributes.deferredCallableCopyReturnType] 与声明属性关联。
 */
abstract class CallableCopyTypeCalculator {
    /**
     * 计算 [declaration] 的返回类型 type ref。
     *
     * 实现可以触发 [CfirDeclarationAttributes.deferredCallableCopyReturnType] 中保存的延迟计算；
     * 返回 `null` 表示计算失败，或没有可用的普通 resolved return type。
     */
    abstract fun computeReturnType(declaration: CfirCallableDeclaration): CfirTypeRef?

    /**
     * 计算 callable copy 返回类型；失败时返回 `null`。
     */
    fun computeReturnTypeOrNull(declaration: CfirCallableDeclaration): ConeCangJieType? {
        return computeReturnType(declaration)?.coneTypeOrNull
    }

    /**
     * 不执行额外计算，直接返回 [CfirCallableDeclaration.returnTypeRef]。
     */
    object DoNothing : CallableCopyTypeCalculator() {
        /**
         * 返回声明当前保存的 return type ref。
         */
        override fun computeReturnType(declaration: CfirCallableDeclaration): CfirTypeRef {
            return declaration.returnTypeRef
        }
    }

    /**
     * 在必要时执行 [CfirDeclarationAttributes.deferredCallableCopyReturnType] 中保存的延迟计算。
     */
    abstract class DeferredCallableCopyTypeCalculator : CallableCopyTypeCalculator() {
        /**
         * 计算并写回 callable copy 的 resolved return type ref。
         */
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

        /**
         * 返回声明当前已解析的 return type ref。
         */
        protected abstract fun CfirCallableDeclaration.getResolvedTypeRef(): CfirResolvedTypeRef?
    }

    /**
     * 执行延迟返回类型计算，并强制 lazy resolve 被覆盖声明。
     *
     * @see DeferredCallableCopyTypeCalculator
     * @see CfirDeclarationAttributes.deferredCallableCopyReturnType
     */
    object CalculateDeferredForceLazyResolution : DeferredCallableCopyTypeCalculator() {
        /**
         * 通过 symbol 强制取得 resolved return type ref。
         */
        override fun CfirCallableDeclaration.getResolvedTypeRef(): CfirResolvedTypeRef? {
            return symbol.resolvedReturnTypeRef
        }
    }

    /**
     * 执行延迟返回类型计算，但不强制 lazy resolve 被覆盖声明。
     *
     * @see DeferredCallableCopyTypeCalculator
     * @see CfirDeclarationAttributes.deferredCallableCopyReturnType
     */
    object CalculateDeferredWhenPossible : DeferredCallableCopyTypeCalculator() {
        /**
         * 仅在 return type ref 已解析时返回。
         */
        override fun CfirCallableDeclaration.getResolvedTypeRef(): CfirResolvedTypeRef? {
            return returnTypeRef as? CfirResolvedTypeRef
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------------

/**
 * 延迟 callable copy 返回类型属性 key。
 */
private object DeferredCallableCopyReturnTypeKey : CfirDeclarationDataKey()

/**
 * callable copy 返回类型的延迟计算属性。
 */
var CfirDeclarationAttributes.deferredCallableCopyReturnType: DeferredCallableCopyReturnType? by CfirDeclarationDataRegistry.attributesAccessor(
    DeferredCallableCopyReturnTypeKey
)

/**
 * 延迟 callable copy 返回类型计算。
 */
abstract class DeferredCallableCopyReturnType {
    /**
     * 执行某个声明返回类型的延迟计算。
     *
     * 计算覆盖成员返回类型时必须通过 [calc]，以便递归触发其他延迟返回类型计算。
     */
    abstract fun computeReturnType(calc: CallableCopyTypeCalculator): ConeCangJieType?
}
