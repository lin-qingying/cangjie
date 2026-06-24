package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 无额外子节点的 argument list 基类。
 *
 * 该类适用于空参数列表或不需要在树变换中重写参数节点的轻量 argument list 实现。
 */
abstract class CfirAbstractArgumentList : CfirArgumentList() {
    /**
     * 默认不转换参数，直接返回当前 argument list。
     */
    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirArgumentList {
        return this
    }

    /**
     * 默认没有需要访问的子节点。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        // DO NOTHING
    }

    /**
     * 默认没有需要转换的子节点。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        return this
    }
}

/**
 * 已完成实参到形参映射的 argument list。
 *
 * 解析后的调用不再只关心源码顺序中的表达式列表，还需要保存 value 参数映射、
 * context 参数映射和原始 argument list，以便 call completer、checker 与 renderer 共享同一结果。
 */
abstract class CfirResolvedArgumentList : CfirArgumentList() {
    /**
     * 已解析参数列表沿用原始参数列表的源码位置。
     */
    final override val source: CjSourceElement?
        get() = originalArgumentList?.source

    /**
     * 解析前的原始参数列表；错误恢复或 synthetic 调用中可能为空。
     */
    abstract val originalArgumentList: CfirArgumentList?

    /**
     * **值实参** 到 **值形参** 的映射。
     *
     * 迭代顺序对应源码中的原始实参顺序，但跳过 context 实参。
     *
     * 若需要包含 context 实参的完整映射，使用 [mappingIncludingContextArguments]。
     */
    abstract val mapping: LinkedHashMap<CfirExpression, CfirValueParameter>

    /**
     * 所有显式实参到 context/value 形参的映射。
     *
     * 该映射包含显式 context 实参，但不包含隐式 context 实参；迭代顺序对应源码中的原始实参顺序。
     *
     * 若只需要值实参映射，使用 [mapping]。
     */
    abstract val mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter>

    /**
     * 按 value 参数映射顺序暴露实参表达式。
     */
    override val arguments: List<CfirExpression>
        get() = mapping.keys.toList()


    /**
     * 转换已解析参数列表中的实参表达式。
     */
    abstract override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirArgumentList

    /**
     * 转换实参表达式并返回当前节点。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        transformArguments(transformer, data)
        return this
    }

    /**
     * 按解析后的实参顺序访问子表达式。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        for (argument in arguments) {
            argument.accept(visitor, data)
        }
    }

}

/**
 * 正常调用的已解析参数列表实现。
 *
 * @property originalArgumentList 解析前的原始参数列表。
 * @property mapping value 实参到 value 形参的映射。
 * @property mappingIncludingContextArguments 包含显式 context 实参的完整映射。
 */
internal class CfirResolvedArgumentListImpl(
    override val originalArgumentList: CfirArgumentList?,
    mapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
    mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter> = mapping,
) : CfirResolvedArgumentList() {
    /**
     * 包含显式 context 实参的完整映射。
     */
    override var mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter> =
        mappingIncludingContextArguments
        private set

    /**
     * value 实参到 value 形参的映射。
     */
    override var mapping: LinkedHashMap<CfirExpression, CfirValueParameter> = mapping
        private set

    /**
     * 转换映射 key 中的实参表达式，并保持原有形参映射关系。
     */
    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirArgumentList {
        mappingIncludingContextArguments =
            mappingIncludingContextArguments.mapKeys { (k, _) -> k.transformSingle(transformer, data) } as LinkedHashMap<CfirExpression, CfirValueParameter>
        mapping =
            mapping.mapKeys { (k, _) -> k.transformSingle(transformer, data) } as LinkedHashMap<CfirExpression, CfirValueParameter>
        return this
    }
}

/**
 * 错误调用使用的已解析参数列表实现。
 *
 * 错误调用可能存在无法绑定到有效形参的实参，因此内部 [_mapping] 允许形参为空；
 * 对外暴露的 [mapping] 会过滤掉这些空形参条目。
 */
internal class CfirResolvedArgumentListForErrorCall(
    /**
     * 解析前的原始参数列表。
     */
    override val originalArgumentList: CfirArgumentList?,
    /**
     * 错误调用的原始实参映射，值为 `null` 表示该实参没有可用形参。
     */
    private var _mapping: LinkedHashMap<CfirExpression, out CfirValueParameter?>,
) : CfirResolvedArgumentList() {

    /**
     * 过滤空形参后的 value 实参映射。
     */
    override var mapping: LinkedHashMap<CfirExpression, CfirValueParameter> = computeMapping()
        private set

    /**
     * 错误调用没有单独的 context 映射，对外复用 [mapping]。
     */
    override val mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter>
        get() = mapping

    /**
     * 从 [_mapping] 中过滤掉没有形参的错误实参条目。
     */
    private fun computeMapping(): LinkedHashMap<CfirExpression, CfirValueParameter> {
        @Suppress("UNCHECKED_CAST")
        return _mapping.filterValues { it != null } as LinkedHashMap<CfirExpression, CfirValueParameter>
    }

    /**
     * 错误调用保留所有原始实参表达式，包括未绑定到形参的条目。
     */
    override val arguments: List<CfirExpression>
        get() = _mapping.keys.toList()

    /**
     * 转换错误调用中的所有实参表达式，并重新计算过滤后的映射。
     */
    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirResolvedArgumentListForErrorCall {
        _mapping = _mapping.mapKeys { (k, _) -> k.transformSingle(transformer, data) } as LinkedHashMap<CfirExpression, CfirValueParameter?>
        mapping = computeMapping()
        return this
    }
}

/**
 * 构建正常调用的已解析参数列表。
 */
fun buildResolvedArgumentList(
    originalArgumentList: CfirArgumentList,
    mapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
    mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter> = mapping,
): CfirResolvedArgumentList {
    return CfirResolvedArgumentListImpl(originalArgumentList, mapping, mappingIncludingContextArguments)
}

/**
 * 构建错误调用的已解析参数列表。
 *
 * [mapping] 中的 `null` 形参会在对外 [CfirResolvedArgumentList.mapping] 中被过滤。
 */
fun buildArgumentListForErrorCall(
    originalArgumentList: CfirArgumentList,
    mapping: LinkedHashMap<CfirExpression, out CfirValueParameter?>,
): CfirResolvedArgumentList {
    return CfirResolvedArgumentListForErrorCall(originalArgumentList, mapping)
}
