package org.cangnova.cangjie.cfir.types.impl

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 已知类型的隐式类型引用。
 *
 * 某些声明虽然在隐式类型阶段仍需要参与 resolve，但其最终类型已经由上下文确定。
 * 该节点把已解析的 [typeRef] 包装成 [CfirImplicitTypeRef]，让调用方仍按“隐式类型引用”
 * 处理 resolve 流程，同时避免重复推断同一个类型。
 *
 * @property typeRef 已知的已解析类型引用。
 */
class ResolvedImplicitTypeRef(
    val typeRef: CfirResolvedTypeRef,
) : CfirImplicitTypeRef() {
    /**
     * 该包装节点不启用自定义 renderer。
     */
    override val customRenderer: Boolean
        get() = false

    /**
     * 包装节点自身没有独立源码位置。
     */
    override val source: CjSourceElement? get() = null

    /**
     * 包装节点不额外携带注解。
     */
    override val annotations: List<CfirAnnotation> get() = emptyList()

    /**
     * 已解析隐式类型引用不接受注解替换。
     */
    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
    }

    /**
     * 已解析隐式类型引用没有可转换注解，直接返回自身。
     */
    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirImplicitTypeRef {
        return this
    }

    /**
     * 该包装节点不把 [typeRef] 暴露为普通子节点，避免破坏隐式类型引用的消费契约。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
    }

    /**
     * 包装节点没有需要递归转换的子节点。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        return this
    }
}
