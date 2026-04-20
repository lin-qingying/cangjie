

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirAnnotationCallImpl(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var typeRef: CfirTypeRef,
    override val arguments: MutableList<CfirElement>,
    override var argumentList: CfirArgumentList,
    override var calleeReference: CfirReference,
    override val containingDeclarationSymbol: CfirBasedSymbol<*>,
) : CfirAnnotationCall() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeRef.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirAnnotationCallImpl {
        transformAnnotations(transformer, data)
        transformTypeRef(transformer, data)
        transformArguments(transformer, data)
        argumentList = argumentList.transform(transformer, data)
        transformCalleeReference(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAnnotationCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirAnnotationCallImpl {
        typeRef = typeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirAnnotationCallImpl {
        arguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirAnnotationCallImpl {
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceArgumentList(newArgumentList: CfirArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceCalleeReference(newCalleeReference: CfirReference) {
        calleeReference = newCalleeReference
    }
}
