

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.annotations.CangjieAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.annotationCall]
 */
abstract class CfirAnnotationCall : CfirAnnotation(), CfirCall, CfirResolvable {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract override val typeRef: CfirTypeRef
    abstract override val arguments: List<CfirElement>
    abstract override val annotationKind: CangjieAnnotationKind?
    abstract override val annotationOrigin: CangjieAnnotationOrigin?
    abstract override val isCompileTimeVisible: Boolean?
    abstract override val argumentList: CfirArgumentList
    abstract override val calleeReference: CfirReference
    abstract val argumentView: CfirAnnotationArgumentView?
    abstract val annotationResolveState: CfirAnnotationResolveState
    abstract val containingDeclarationSymbol: CfirBasedSymbol<*>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitAnnotationCall(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformAnnotationCall(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun replaceAnnotationKind(newAnnotationKind: CangjieAnnotationKind?)

    abstract override fun replaceAnnotationOrigin(newAnnotationOrigin: CangjieAnnotationOrigin?)

    abstract override fun replaceIsCompileTimeVisible(newIsCompileTimeVisible: Boolean?)

    abstract override fun replaceArgumentList(newArgumentList: CfirArgumentList)

    abstract override fun replaceCalleeReference(newCalleeReference: CfirReference)

    abstract fun replaceArgumentView(newArgumentView: CfirAnnotationArgumentView?)

    abstract fun replaceAnnotationResolveState(newAnnotationResolveState: CfirAnnotationResolveState)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAnnotationCall

    abstract override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirAnnotationCall

    abstract override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirAnnotationCall

    abstract override fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirAnnotationCall
}
