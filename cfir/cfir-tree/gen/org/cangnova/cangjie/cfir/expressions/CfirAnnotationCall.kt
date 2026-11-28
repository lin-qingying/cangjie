

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.annotationCall]
 */
abstract class CfirAnnotationCall : CfirAnnotation(), CfirCall, CfirResolvable {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract override val mappedExpression: CfirExpression?
    abstract override val overflowStrategy: CangjieOverflowStrategy?
    abstract override val typeRef: CfirTypeRef
    abstract override val arguments: List<CfirExpression>
    abstract override val argumentMapping: CfirAnnotationArgumentMapping
    abstract override val annotationClassId: ClassId?
    abstract override val annotationTarget: CangjieAnnotationTarget?
    abstract override val forcedCustom: Boolean
    abstract override val annotationSourceName: String?
    abstract override val sourceModuleName: String
    abstract override val annotationKind: BuiltInAnnotationKind?
    abstract override val annotationIdentity: CangjieAnnotationIdentity?
    abstract override val annotationOrigin: CangjieAnnotationOrigin?
    abstract override val isCompileTimeVisible: Boolean?
    abstract override val argumentList: CfirArgumentList
    abstract override val calleeReference: CfirReference
    abstract val argumentView: CfirAnnotationArgumentView?
    abstract val annotationResolveState: CfirAnnotationResolveState
    abstract val containingDeclarationSymbol: CfirBasedSymbol<*>

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeRef.accept(visitor, data)
        argumentMapping.accept(visitor, data)
        argumentList.accept(visitor, data)
        calleeReference.accept(visitor, data)
        argumentView?.acceptSourceChildren(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitAnnotationCall(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformAnnotationCall(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun replaceMappedExpression(newMappedExpression: CfirExpression?)

    abstract override fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?)

    abstract override fun replaceTypeRef(newTypeRef: CfirTypeRef)

    abstract override fun replaceArgumentMapping(newArgumentMapping: CfirAnnotationArgumentMapping)

    abstract override fun replaceAnnotationClassId(newAnnotationClassId: ClassId?)

    abstract override fun replaceAnnotationTarget(newAnnotationTarget: CangjieAnnotationTarget?)

    abstract override fun replaceForcedCustom(newForcedCustom: Boolean)

    abstract override fun replaceAnnotationSourceName(newAnnotationSourceName: String?)

    abstract override fun replaceSourceModuleName(newSourceModuleName: String)

    abstract override fun replaceAnnotationKind(newAnnotationKind: BuiltInAnnotationKind?)

    abstract override fun replaceAnnotationIdentity(newAnnotationIdentity: CangjieAnnotationIdentity?)

    abstract override fun replaceAnnotationOrigin(newAnnotationOrigin: CangjieAnnotationOrigin?)

    abstract override fun replaceIsCompileTimeVisible(newIsCompileTimeVisible: Boolean?)

    abstract override fun replaceArgumentList(newArgumentList: CfirArgumentList)

    abstract override fun replaceCalleeReference(newCalleeReference: CfirReference)

    abstract fun replaceArgumentView(newArgumentView: CfirAnnotationArgumentView?)

    abstract fun replaceAnnotationResolveState(newAnnotationResolveState: CfirAnnotationResolveState)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAnnotationCall

    abstract override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirAnnotationCall

    abstract override fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirAnnotationCall
}
