package org.cangnova.cangjie.cfir.references.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 已应用类型替换后的 callable 引用。
 *
 * 普通 resolved reference 只记录命中的符号；该实现额外保存 use-site 替换后的返回类型和参数类型，
 * 供调用解析、签名渲染和后续诊断在不重新构造 substitutor 的情况下消费。
 *
 * @property source 引用源码位置。
 * @property name 被解析的 callable 名称。
 * @property resolvedSymbol 解析命中的 callable 符号。
 * @property substitutedReturnType 替换后的返回类型；没有可用返回类型时为 `null`。
 * @property substitutedParameterTypes 替换后的参数类型列表。
 */
class CfirResolvedAppliedCallableReference @CfirImplementationDetail constructor(
    /**
     * 引用表达式在源码中的位置。
     */
    override val source: CjSourceElement?,
    /**
     * 源码中被解析的 callable 名称。
     */
    override val name: Name,
    /**
     * 解析命中的目标符号。
     */
    override val resolvedSymbol: CfirBasedSymbol<*>,
    /**
     * 按 use-site 类型实参替换后的返回类型。
     */
    val substitutedReturnType: ConeCangJieType?,
    /**
     * 按 use-site 类型实参替换后的参数类型列表。
     */
    val substitutedParameterTypes: List<ConeCangJieType>,
) : CfirResolvedNamedReference {

    /**
     * 已解析 callable 引用没有 CFIR 子节点。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
    }

    /**
     * 已解析 callable 引用没有可转换子节点，直接返回自身。
     */
    override fun <D> transformChildren(
        transformer: CfirTransformer<D>,
        data: D,
    ): CfirResolvedAppliedCallableReference {
        return this
    }
}
