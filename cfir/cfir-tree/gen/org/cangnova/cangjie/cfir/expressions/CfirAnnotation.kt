

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.annotation]
 */
abstract class CfirAnnotation : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract override val mappedExpression: CfirExpression?
    abstract override val overflowStrategy: CangjieOverflowStrategy?
    abstract val typeRef: CfirTypeRef
    abstract val arguments: List<CfirExpression>
    abstract val argumentMapping: CfirAnnotationArgumentMapping
    abstract val annotationClassId: ClassId?
    abstract val annotationTarget: CangjieAnnotationTarget?
    abstract val forcedCustom: Boolean
    abstract val annotationSourceName: String?
    abstract val sourceModuleName: String
    abstract val annotationKind: BuiltInAnnotationKind?
    abstract val annotationIdentity: CangjieAnnotationIdentity?
    abstract val annotationOrigin: CangjieAnnotationOrigin?
    abstract val isCompileTimeVisible: Boolean?

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeRef.accept(visitor, data)
        argumentMapping.accept(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitAnnotation(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformAnnotation(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun replaceMappedExpression(newMappedExpression: CfirExpression?)

    abstract override fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?)

    abstract fun replaceTypeRef(newTypeRef: CfirTypeRef)

    abstract fun replaceArgumentMapping(newArgumentMapping: CfirAnnotationArgumentMapping)

    abstract fun replaceAnnotationClassId(newAnnotationClassId: ClassId?)

    abstract fun replaceAnnotationTarget(newAnnotationTarget: CangjieAnnotationTarget?)

    abstract fun replaceForcedCustom(newForcedCustom: Boolean)

    abstract fun replaceAnnotationSourceName(newAnnotationSourceName: String?)

    abstract fun replaceSourceModuleName(newSourceModuleName: String)

    abstract fun replaceAnnotationKind(newAnnotationKind: BuiltInAnnotationKind?)

    abstract fun replaceAnnotationIdentity(newAnnotationIdentity: CangjieAnnotationIdentity?)

    abstract fun replaceAnnotationOrigin(newAnnotationOrigin: CangjieAnnotationOrigin?)

    abstract fun replaceIsCompileTimeVisible(newIsCompileTimeVisible: Boolean?)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAnnotation

    abstract fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirAnnotation
}
