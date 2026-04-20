package org.cangnova.cangjie.cfir.calls

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirInaccessibleReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartcastStability
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.buildInaccessibleReceiverExpressionCopy
import org.cangnova.cangjie.cfir.expressions.buildSmartCastExpression
import org.cangnova.cangjie.cfir.expressions.buildSmartCastExpressionCopy
import org.cangnova.cangjie.cfir.expressions.buildThisReceiverExpressionCopy
import org.cangnova.cangjie.cfir.expressions.unwrapSmartcastExpression
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

sealed class ImplicitValue<S : CfirBasedSymbol<*>>(
    type: ConeCangJieType,
    val originalType: ConeCangJieType,
    protected val mutable: Boolean,
) {
    abstract val boundSymbol: S

    var type: ConeCangJieType = type
        private set

    protected abstract fun computeOriginalExpression(): CfirExpression

    protected val originalExpression: CfirExpression by lazy(
        LazyThreadSafetyMode.PUBLICATION,
        ::computeOriginalExpression,
    )

    private var isSmartCasted: Boolean = type != originalType

    fun computeExpression(): CfirExpression {
        return if (isSmartCasted) {
            buildSmartCastExpression {
                source = this@ImplicitValue.originalExpression.source
                    ?.fakeElement(CjFakeSourceElementKind.SmartCastExpression)
                originalExpression = this@ImplicitValue.originalExpression.copyImplicitValueExpression()
                smartcastType = buildResolvedTypeRef {
                    source = this@ImplicitValue.originalExpression.source
                        ?.fakeElement(CjFakeSourceElementKind.SmartCastedTypeRef)
                    coneType = this@ImplicitValue.type
                }
                upperTypesFromSmartCast = listOf(this@ImplicitValue.type)
                lowerTypesFromSmartCast = emptyList()
                smartcastStability = CfirSmartcastStability.STABLE_VALUE
                coneTypeOrNull = this@ImplicitValue.type
            }
        } else {
            originalExpression
        }
    }

    fun isSameImplicitReceiverInstance(other: CfirExpression): Boolean {
        val otherBoundSymbol = when (val otherOriginal = other.unwrapSmartcastExpression()) {
            is CfirThisReceiverExpression -> otherOriginal.calleeReference.boundSymbol
            else -> null
        }

        return boundSymbol === otherBoundSymbol
    }

    @RequiresOptIn
    annotation class ImplicitValueInternals

    @ImplicitValueInternals
    open fun updateTypeFromSmartcast(type: ConeCangJieType) {
        if (type == this.type) return
        if (!mutable) error("Cannot mutate an immutable ImplicitValue")
        this.type = type
        isSmartCasted = type != originalType
    }

    abstract fun createSnapshot(keepMutable: Boolean): ImplicitValue<S>
}

fun CfirExpression.copyImplicitValueExpression(): CfirExpression {
    return when (this) {
        is CfirThisReceiverExpression -> buildThisReceiverExpressionCopy(this) {}
        is CfirInaccessibleReceiverExpression -> buildInaccessibleReceiverExpressionCopy(this) {}
        is CfirSmartCastExpression -> buildSmartCastExpressionCopy(this) {}
        else -> error("Unexpected expression type '${this.javaClass.simpleName}'")
    }
}
