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

/**
 * 隐式值的公共基类。
 *
 * 隐式值保存原始类型、当前 smartcast 后类型以及绑定 symbol，用于 this/extend receiver
 * 在解析过程中的类型更新和表达式重建。
 *
 * @property originalType 隐式值进入解析时的原始类型。
 * @property mutable 当前隐式值是否允许被 smartcast 更新。
 */
sealed class ImplicitValue<S : CfirBasedSymbol<*>>(
    type: ConeCangJieType,
    /**
     * 隐式值进入解析时的原始类型，smartcast 回退与表达式重建都以它作为基准。
     */
    val originalType: ConeCangJieType,
    /**
     * 是否允许当前隐式值在数据流分析中被 smartcast 更新。
     */
    protected val mutable: Boolean,
) {
    /**
     * 当前隐式值绑定的声明 symbol。
     */
    abstract val boundSymbol: S

    /**
     * 当前可见类型；发生 smartcast 后会从 [originalType] 更新为收窄后的类型。
     */
    var type: ConeCangJieType = type
        private set

    /**
     * 构造未 smartcast 时的原始 CFIR 表达式。
     */
    protected abstract fun computeOriginalExpression(): CfirExpression

    /**
     * 缓存后的原始表达式。
     */
    protected val originalExpression: CfirExpression by lazy(
        LazyThreadSafetyMode.PUBLICATION,
        ::computeOriginalExpression,
    )

    /**
     * 当前隐式值是否已经应用 smartcast 类型。
     */
    private var isSmartCasted: Boolean = type != originalType

    /**
     * 返回当前隐式值对应的 CFIR 表达式。
     *
     * 若类型已被 smartcast，则包装为 [CfirSmartCastExpression]；否则直接返回原始表达式。
     */
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

    /**
     * 判断 [other] 是否引用同一个隐式 receiver 实例。
     */
    fun isSameImplicitReceiverInstance(other: CfirExpression): Boolean {
        val otherBoundSymbol = when (val otherOriginal = other.unwrapSmartcastExpression()) {
            is CfirThisReceiverExpression -> otherOriginal.calleeReference.boundSymbol
            else -> null
        }

        return boundSymbol === otherBoundSymbol
    }

    /**
     * 标记隐式值可变更新的内部 API。
     */
    @RequiresOptIn
    annotation class ImplicitValueInternals

    /**
     * 将当前隐式值类型更新为 smartcast 后类型。
     *
     * 不可变快照禁止更新，用于防止候选复制阶段污染原始 receiver 状态。
     */
    @ImplicitValueInternals
    open fun updateTypeFromSmartcast(type: ConeCangJieType) {
        if (type == this.type) return
        if (!mutable) error("Cannot mutate an immutable ImplicitValue")
        this.type = type
        isSmartCasted = type != originalType
    }

    /**
     * 创建当前隐式值的快照。
     *
     * [keepMutable] 控制快照是否继续允许 smartcast 更新。
     */
    abstract fun createSnapshot(keepMutable: Boolean): ImplicitValue<S>
}

/**
 * 复制隐式值表达式，保留 receiver/smartcast 结构但断开可变节点共享。
 */
fun CfirExpression.copyImplicitValueExpression(): CfirExpression {
    return when (this) {
        is CfirThisReceiverExpression -> buildThisReceiverExpressionCopy(this) {}
        is CfirInaccessibleReceiverExpression -> buildInaccessibleReceiverExpressionCopy(this) {}
        is CfirSmartCastExpression -> buildSmartCastExpressionCopy(this) {}
        else -> error("Unexpected expression type '${this.javaClass.simpleName}'")
    }
}
