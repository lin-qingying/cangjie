package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.source.CjSourceElement
abstract class CfirAbstractArgumentList : CfirArgumentList(){
    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirArgumentList {
        return this
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        // DO NOTHING
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        return this
    }
}
abstract class CfirResolvedArgumentList: CfirArgumentList() {
    final override val source: CjSourceElement?
        get() = originalArgumentList?.source

    abstract val originalArgumentList: CfirArgumentList?
    /**
     * Contains the mapping of **value** arguments to **value** parameters.
     *
     * The iteration order corresponds to the original argument order in the source skipping context arguments.
     *
     * For the complete mapping including context arguments, see [mappingIncludingContextArguments].
     */
    abstract val mapping: LinkedHashMap<CfirExpression, CfirValueParameter>
    /**
     * Contains the mapping of all arguments including explicit (but not implicit) context arguments to context/value parameters.
     *
     * The iteration order corresponds to the original argument order in the source.
     *
     * For the mapping of value arguments only, see [mapping].
     */
    abstract val mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter>

    override val arguments: List<CfirExpression>
        get() = mapping.keys.toList()


    abstract override fun <D> transformArguments(transformer:CfirTransformer<D>, data: D): CfirArgumentList

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        transformArguments(transformer, data)
        return this
    }
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        for (argument in arguments) {
            argument.accept(visitor, data)
        }
    }

}

internal class CfirResolvedArgumentListImpl(
    override val originalArgumentList: CfirArgumentList?,
    mapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
    mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter> = mapping,
) : CfirResolvedArgumentList() {
    override var mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter> =
        mappingIncludingContextArguments
        private set

    override var mapping: LinkedHashMap<CfirExpression, CfirValueParameter> = mapping
        private set

    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirArgumentList {
        mappingIncludingContextArguments =
            mappingIncludingContextArguments.mapKeys { (k, _) -> k.transformSingle(transformer, data) } as LinkedHashMap<CfirExpression, CfirValueParameter>
        mapping =
            mapping.mapKeys { (k, _) -> k.transformSingle(transformer, data) } as LinkedHashMap<CfirExpression, CfirValueParameter>
        return this
    }
}

internal class CfirResolvedArgumentListForErrorCall(
    override val originalArgumentList: CfirArgumentList?,
    private var _mapping: LinkedHashMap<CfirExpression, out CfirValueParameter?>,
) : CfirResolvedArgumentList() {

    override var mapping: LinkedHashMap<CfirExpression, CfirValueParameter> = computeMapping()
        private set

    override val mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter>
        get() = mapping

    private fun computeMapping(): LinkedHashMap<CfirExpression, CfirValueParameter> {
        @Suppress("UNCHECKED_CAST")
        return _mapping.filterValues { it != null } as LinkedHashMap<CfirExpression, CfirValueParameter>
    }

    override val arguments: List<CfirExpression>
        get() = _mapping.keys.toList()

    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirResolvedArgumentListForErrorCall {
        _mapping = _mapping.mapKeys { (k, _) -> k.transformSingle(transformer, data) } as LinkedHashMap<CfirExpression, CfirValueParameter?>
        mapping = computeMapping()
        return this
    }
}

fun buildResolvedArgumentList(
    originalArgumentList: CfirArgumentList,
    mapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
    mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter> = mapping,
): CfirResolvedArgumentList {
    return CfirResolvedArgumentListImpl(originalArgumentList, mapping, mappingIncludingContextArguments)
}

fun buildArgumentListForErrorCall(
    originalArgumentList: CfirArgumentList,
    mapping: LinkedHashMap<CfirExpression, out CfirValueParameter?>,
): CfirResolvedArgumentList {
    return CfirResolvedArgumentListForErrorCall(originalArgumentList, mapping)
}
