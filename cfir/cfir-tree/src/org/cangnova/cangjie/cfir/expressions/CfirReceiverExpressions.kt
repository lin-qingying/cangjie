package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReferenceCopy
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 无法作为普通值访问的 receiver 种类。
 */
enum class InaccessibleReceiverKind {
    /**
     * class header 中的 receiver。
     */
    CLASS_HEADER,

    /**
     * 嵌套 class 场景中的 receiver。
     */
    NESTED_CLASS,

    /**
     * static 成员上下文中的 receiver。
     */
    STATIC_MEMBER,

    /**
     * enum constructor 上下文中的 receiver。
     */
    ENUM_CONSTRUCTOR,

    /**
     * finalizer 上下文中的 receiver。
     */
    FINALIZER;

    /**
     * 当前 inaccessible receiver 是否仍产出可适用候选。
     */
    val producesApplicableCandidate: Boolean
        get() = when (this) {
            // finalizer 中成员访问仍然合法，只禁止把 `this` 当值直接使用。
            FINALIZER -> true
            else -> false
        }
}

/**
 * smartcast 稳定性分类。
 */
enum class CfirSmartcastStability {
    /**
     * smartcast 目标是稳定值。
     */
    STABLE_VALUE,

    /**
     * smartcast 目标是不稳定值。
     */
    UNSTABLE_VALUE,
}

/**
 * `this` receiver 表达式。
 *
 * @property source 源码位置。
 * @property annotations 表达式注解。
 * @property coneTypeOrNull 表达式类型。
 * @property calleeReference `this` 引用。
 */
open class CfirThisReceiverExpression(
    /**
     * 源码位置。
     */
    override val source: CjSourceElement?,
    /**
     * 表达式注解。
     */
    override var annotations: List<CfirAnnotation>,
    /**
     * 表达式类型。
     */
    override var coneTypeOrNull: ConeCangJieType?,
    /**
     * `this` 引用。
     */
    open var calleeReference: CfirThisReference,
) : CfirExpression() {
    /**
     * 接受普通表达式 visitor。
     */
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitExpression(this, data)

    /**
     * 使用普通表达式 transformer 转换 receiver。
     */
    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformExpression(this, data) as E

    /**
     * 替换表达式注解。
     */
    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations
    }

    /**
     * 替换表达式类型。
     */
    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    /**
     * 访问注解和 `this` 引用子节点。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
    }

    /**
     * 转换注解和 `this` 引用子节点。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirThisReceiverExpression {
        annotations = annotations.map { it.transform(transformer, data) }
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    /**
     * 转换表达式注解。
     */
    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirExpression {
        annotations = annotations.map { it.transform(transformer, data) }
        return this
    }
}

/**
 * 无法作为普通值访问的 `this` receiver 表达式。
 */
class CfirInaccessibleReceiverExpression(
    source: CjSourceElement?,
    annotations: List<CfirAnnotation>,
    coneTypeOrNull: ConeCangJieType?,
    calleeReference: CfirThisReference,
    /**
     * inaccessible receiver 种类。
     */
    var kind: InaccessibleReceiverKind,
) : CfirThisReceiverExpression(source, annotations, coneTypeOrNull, calleeReference)

/**
 * smartcast 包装表达式。
 *
 * @property source 源码位置。
 * @property annotations 表达式注解。
 * @property coneTypeOrNull 表达式类型。
 * @property originalExpression smartcast 前的原始表达式。
 * @property smartcastType smartcast 后的类型引用。
 * @property upperTypesFromSmartCast smartcast 推导出的上界类型集合。
 * @property lowerTypesFromSmartCast smartcast 推导出的下界类型集合。
 * @property smartcastStability smartcast 稳定性。
 */
class CfirSmartCastExpression(
    /**
     * 源码位置。
     */
    override val source: CjSourceElement?,
    /**
     * 表达式注解。
     */
    override var annotations: List<CfirAnnotation>,
    /**
     * 表达式类型。
     */
    override var coneTypeOrNull: ConeCangJieType?,
    /**
     * smartcast 前的原始表达式。
     */
    var originalExpression: CfirExpression,
    /**
     * smartcast 后的类型引用。
     */
    var smartcastType: CfirTypeRef,
    /**
     * smartcast 推导出的上界类型集合。
     */
    var upperTypesFromSmartCast: List<ConeCangJieType>,
    /**
     * smartcast 推导出的下界类型集合。
     */
    var lowerTypesFromSmartCast: List<ConeCangJieType>,
    /**
     * smartcast 稳定性。
     */
    var smartcastStability: CfirSmartcastStability,

) : CfirExpression() {
    /**
     * 接受普通表达式 visitor。
     */
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitExpression(this, data)

    /**
     * 使用普通表达式 transformer 转换 smartcast 表达式。
     */
    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformExpression(this, data) as E

    /**
     * 替换表达式注解。
     */
    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations
    }

    /**
     * 替换表达式类型。
     */
    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    /**
     * 访问 smartcast 表达式子节点。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        originalExpression.accept(visitor, data)
        smartcastType.accept(visitor, data)
    }

    /**
     * 转换 smartcast 表达式子节点。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirSmartCastExpression {
        annotations = annotations.map { it.transform(transformer, data) }
        originalExpression = originalExpression.transform(transformer, data)
        smartcastType = smartcastType.transform(transformer, data)
        return this
    }

    /**
     * 转换表达式注解。
     */
    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirExpression {
        annotations = annotations.map { it.transform(transformer, data) }
        return this
    }
}

/**
 * 去除连续 smartcast 包装，返回原始表达式。
 */
fun CfirExpression.unwrapSmartcastExpression(): CfirExpression {
    var current = this
    while (current is CfirSmartCastExpression) {
        current = current.originalExpression
    }
    return current
}

/**
 * [CfirThisReceiverExpression] 构建器。
 */
class CfirThisReceiverExpressionBuilder {
    /**
     * 源码位置。
     */
    var source: CjSourceElement? = null

    /**
     * 表达式注解。
     */
    var annotations: List<CfirAnnotation> = emptyList()

    /**
     * 表达式类型。
     */
    var coneTypeOrNull: ConeCangJieType? = null

    /**
     * `this` 引用。
     */
    lateinit var calleeReference: CfirThisReference

    /**
     * 构建 `this` receiver 表达式。
     */
    fun build(): CfirThisReceiverExpression =
        CfirThisReceiverExpression(source, annotations, coneTypeOrNull, calleeReference)
}

/**
 * 使用 DSL 构建 `this` receiver 表达式。
 */
inline fun buildThisReceiverExpression(init: CfirThisReceiverExpressionBuilder.() -> Unit): CfirThisReceiverExpression =
    CfirThisReceiverExpressionBuilder().apply(init).build()

/**
 * 复制并定制 `this` receiver 表达式。
 */
inline fun buildThisReceiverExpressionCopy(
    original: CfirThisReceiverExpression,
    init: CfirThisReceiverExpressionBuilder.() -> Unit,
): CfirThisReceiverExpression = CfirThisReceiverExpressionBuilder().apply {
    source = original.source
    annotations = original.annotations
    coneTypeOrNull = original.coneTypeOrNull
    calleeReference = buildThisReferenceCopy(original.calleeReference) {}
    init()
}.build()

/**
 * [CfirInaccessibleReceiverExpression] 构建器。
 */
class CfirInaccessibleReceiverExpressionBuilder {
    /**
     * 源码位置。
     */
    var source: CjSourceElement? = null

    /**
     * 表达式注解。
     */
    var annotations: List<CfirAnnotation> = emptyList()

    /**
     * 表达式类型。
     */
    var coneTypeOrNull: ConeCangJieType? = null

    /**
     * `this` 引用。
     */
    lateinit var calleeReference: CfirThisReference

    /**
     * inaccessible receiver 种类。
     */
    var kind: InaccessibleReceiverKind = InaccessibleReceiverKind.CLASS_HEADER

    /**
     * 构建 inaccessible receiver 表达式。
     */
    fun build(): CfirInaccessibleReceiverExpression =
        CfirInaccessibleReceiverExpression(source, annotations, coneTypeOrNull, calleeReference, kind)
}

/**
 * 使用 DSL 构建 inaccessible receiver 表达式。
 */
inline fun buildInaccessibleReceiverExpression(
    init: CfirInaccessibleReceiverExpressionBuilder.() -> Unit,
): CfirInaccessibleReceiverExpression = CfirInaccessibleReceiverExpressionBuilder().apply(init).build()

/**
 * 复制并定制 inaccessible receiver 表达式。
 */
inline fun buildInaccessibleReceiverExpressionCopy(
    original: CfirInaccessibleReceiverExpression,
    init: CfirInaccessibleReceiverExpressionBuilder.() -> Unit,
): CfirInaccessibleReceiverExpression = CfirInaccessibleReceiverExpressionBuilder().apply {
    source = original.source
    annotations = original.annotations
    coneTypeOrNull = original.coneTypeOrNull
    calleeReference = buildThisReferenceCopy(original.calleeReference) {}
    kind = original.kind
    init()
}.build()

/**
 * [CfirSmartCastExpression] 构建器。
 */
class CfirSmartCastExpressionBuilder {
    /**
     * 源码位置。
     */
    var source: CjSourceElement? = null

    /**
     * 表达式注解。
     */
    var annotations: List<CfirAnnotation> = emptyList()

    /**
     * 表达式类型。
     */
    var coneTypeOrNull: ConeCangJieType? = null

    /**
     * smartcast 前的原始表达式。
     */
    lateinit var originalExpression: CfirExpression

    /**
     * smartcast 后的类型引用。
     */
    lateinit var smartcastType: CfirTypeRef

    /**
     * smartcast 推导出的上界类型集合。
     */
    var upperTypesFromSmartCast: List<ConeCangJieType> = emptyList()

    /**
     * smartcast 推导出的下界类型集合。
     */
    var lowerTypesFromSmartCast: List<ConeCangJieType> = emptyList()

    /**
     * smartcast 稳定性。
     */
    var smartcastStability: CfirSmartcastStability = CfirSmartcastStability.STABLE_VALUE

    /**
     * 构建 smartcast 表达式。
     */
    fun build(): CfirSmartCastExpression = CfirSmartCastExpression(
        source = source,
        annotations = annotations,
        coneTypeOrNull = coneTypeOrNull,
        originalExpression = originalExpression,
        smartcastType = smartcastType,
        upperTypesFromSmartCast = upperTypesFromSmartCast,
        lowerTypesFromSmartCast = lowerTypesFromSmartCast,
        smartcastStability = smartcastStability,
    )
}

/**
 * 使用 DSL 构建 smartcast 表达式。
 */
inline fun buildSmartCastExpression(init: CfirSmartCastExpressionBuilder.() -> Unit): CfirSmartCastExpression =
    CfirSmartCastExpressionBuilder().apply(init).build()

/**
 * 复制并定制 smartcast 表达式。
 */
inline fun buildSmartCastExpressionCopy(
    original: CfirSmartCastExpression,
    init: CfirSmartCastExpressionBuilder.() -> Unit,
): CfirSmartCastExpression = CfirSmartCastExpressionBuilder().apply {
    source = original.source
    annotations = original.annotations
    coneTypeOrNull = original.coneTypeOrNull
    originalExpression = original.originalExpression
    smartcastType = original.smartcastType
    upperTypesFromSmartCast = original.upperTypesFromSmartCast
    lowerTypesFromSmartCast = original.lowerTypesFromSmartCast
    smartcastStability = original.smartcastStability
    init()
}.build()
