package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReferenceCopy
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

enum class InaccessibleReceiverKind {
    CLASS_HEADER,
    NESTED_CLASS,
    STATIC_MEMBER,
    ENUM_CONSTRUCTOR;

    val producesApplicableCandidate: Boolean
        get() = false
}

enum class CfirSmartcastStability {
    STABLE_VALUE,
    UNSTABLE_VALUE,
}

open class CfirThisReceiverExpression(
    override val source: CjSourceElement?,
    override var annotations: List<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    open var calleeReference: CfirThisReference,
) : CfirExpression() {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformExpression(this, data) as E

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirThisReceiverExpression {
        annotations = annotations.map { it.transform(transformer, data) }
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirExpression {
        annotations = annotations.map { it.transform(transformer, data) }
        return this
    }
}

class CfirInaccessibleReceiverExpression(
    source: CjSourceElement?,
    annotations: List<CfirAnnotation>,
    coneTypeOrNull: ConeCangJieType?,
    calleeReference: CfirThisReference,
    var kind: InaccessibleReceiverKind,
) : CfirThisReceiverExpression(source, annotations, coneTypeOrNull, calleeReference)

class CfirSmartCastExpression(
    override val source: CjSourceElement?,
    override var annotations: List<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    var originalExpression: CfirExpression,
    var smartcastType: CfirTypeRef,
    var upperTypesFromSmartCast: List<ConeCangJieType>,
    var lowerTypesFromSmartCast: List<ConeCangJieType>,
    var smartcastStability: CfirSmartcastStability,

) : CfirExpression() {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformExpression(this, data) as E

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        originalExpression.accept(visitor, data)
        smartcastType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirSmartCastExpression {
        annotations = annotations.map { it.transform(transformer, data) }
        originalExpression = originalExpression.transform(transformer, data)
        smartcastType = smartcastType.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirExpression {
        annotations = annotations.map { it.transform(transformer, data) }
        return this
    }
}

fun CfirExpression.unwrapSmartcastExpression(): CfirExpression {
    var current = this
    while (current is CfirSmartCastExpression) {
        current = current.originalExpression
    }
    return current
}

class CfirThisReceiverExpressionBuilder {
    var source: CjSourceElement? = null
    var annotations: List<CfirAnnotation> = emptyList()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var calleeReference: CfirThisReference

    fun build(): CfirThisReceiverExpression =
        CfirThisReceiverExpression(source, annotations, coneTypeOrNull, calleeReference)
}

inline fun buildThisReceiverExpression(init: CfirThisReceiverExpressionBuilder.() -> Unit): CfirThisReceiverExpression =
    CfirThisReceiverExpressionBuilder().apply(init).build()

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

class CfirInaccessibleReceiverExpressionBuilder {
    var source: CjSourceElement? = null
    var annotations: List<CfirAnnotation> = emptyList()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var calleeReference: CfirThisReference
    var kind: InaccessibleReceiverKind = InaccessibleReceiverKind.CLASS_HEADER

    fun build(): CfirInaccessibleReceiverExpression =
        CfirInaccessibleReceiverExpression(source, annotations, coneTypeOrNull, calleeReference, kind)
}

inline fun buildInaccessibleReceiverExpression(
    init: CfirInaccessibleReceiverExpressionBuilder.() -> Unit,
): CfirInaccessibleReceiverExpression = CfirInaccessibleReceiverExpressionBuilder().apply(init).build()

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

class CfirSmartCastExpressionBuilder {
    var source: CjSourceElement? = null
    var annotations: List<CfirAnnotation> = emptyList()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var originalExpression: CfirExpression
    lateinit var smartcastType: CfirTypeRef
    var upperTypesFromSmartCast: List<ConeCangJieType> = emptyList()
    var lowerTypesFromSmartCast: List<ConeCangJieType> = emptyList()
    var smartcastStability: CfirSmartcastStability = CfirSmartcastStability.STABLE_VALUE

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

inline fun buildSmartCastExpression(init: CfirSmartCastExpressionBuilder.() -> Unit): CfirSmartCastExpression =
    CfirSmartCastExpressionBuilder().apply(init).build()

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
