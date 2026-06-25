package org.cangnova.cangjie.cfir.types.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 错误类型引用的手写实现。
 *
 * 该节点有意不完全依赖生成式树实现，因为错误类型引用需要保留 Kotlin FIR 风格的自定义遍历语义：
 * [delegatedTypeRef] 表示错误类型背后的委托类型引用，不参与 [acceptChildren] 与 [transformChildren]，
 * 以避免同一类型引用在 visitor / transformer 中被重复访问。
 *
 * @property source 错误类型引用的源码位置。
 * @property annotations 类型引用上的注解。
 * @property delegatedTypeRef 错误类型委托的原始类型引用。
 * @property diagnostic 产生该错误类型的 cone 诊断。
 * @property partiallyResolvedTypeRef 已完成部分解析的类型引用，用于错误恢复和诊断上下文。
 */
class CfirErrorTypeRefImpl @CfirImplementationDetail constructor(
    /**
     * 错误类型引用在源码中的位置。
     */
    override val source: CjSourceElement?,
    /**
     * 错误类型引用携带的类型注解。
     */
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    typeOrNull: ConeCangJieType?,
    /**
     * 触发错误前保留下来的原始或委托类型引用。
     */
    override var delegatedTypeRef: CfirTypeRef?,
    /**
     * 描述错误原因的 cone 诊断。
     */
    override val diagnostic: ConeDiagnostic,
    /**
     * 错误恢复过程中已经局部解析完成的类型引用。
     */
    override var partiallyResolvedTypeRef: CfirTypeRef? = null,
) : CfirErrorTypeRef() {

    /**
     * 错误类型引用暴露给类型系统的 cone 类型。
     *
     * 若构建器没有提供已解析类型，则用 [diagnostic] 创建 [ConeErrorType]。
     */
    override val coneType: ConeCangJieType = typeOrNull ?: ConeErrorType(diagnostic)

    /**
     * 错误类型引用不启用自定义 renderer。
     */
    override val customRenderer: Boolean get() = false

    /**
     * 访问错误类型引用的真实子节点。
     *
     * [delegatedTypeRef] 不在这里访问，避免和委托来源的遍历路径重复。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        partiallyResolvedTypeRef?.accept(visitor, data)
    }

    /**
     * 替换类型注解列表。
     */
    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    /**
     * 原地转换类型注解列表。
     */
    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirErrorTypeRef {
        annotations.transformInplace(transformer, data)

        return this
    }

    /**
     * 转换部分解析出的类型引用，并保持错误类型自身作为返回节点。
     */
    override fun <D> transformPartiallyResolvedTypeRef(transformer: CfirTransformer<D>, data: D): CfirErrorTypeRef {
        partiallyResolvedTypeRef = partiallyResolvedTypeRef?.transform(transformer, data)
        transformChildren(transformer, data)
        return this
    }

    /**
     * 转换错误类型引用的普通子节点。
     *
     * 这里只转换注解；[partiallyResolvedTypeRef] 由专门的 transform 方法处理。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirErrorTypeRef {
        transformAnnotations(transformer, data)
        return this
    }
}
