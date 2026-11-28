

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.errorResolvedQualifier]
 */
abstract class CfirErrorResolvedQualifier : CfirResolvedQualifier(), CfirDiagnosticHolder {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract override val mappedExpression: CfirExpression?
    abstract override val overflowStrategy: CangjieOverflowStrategy?
    abstract override val packageFqName: FqName
    abstract override val relativeClassFqName: FqName?
    abstract override val qualifierSymbol: CfirClassLikeSymbol<*>?
    abstract override val explicitParent: CfirResolvedQualifier?
    abstract override val nonFatalDiagnostics: List<ConeDiagnostic>
    abstract override val typeArguments: List<CfirTypeRef>
    abstract override val organizationName: Name?
    abstract override val referenceName: Name
    abstract override val diagnostic: ConeDiagnostic

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        explicitParent?.accept(visitor, data)
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitErrorResolvedQualifier(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformErrorResolvedQualifier(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract override fun replaceMappedExpression(newMappedExpression: CfirExpression?)

    abstract override fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?)

    abstract override fun replaceExplicitParent(newExplicitParent: CfirResolvedQualifier?)

    abstract override fun replaceNonFatalDiagnostics(newNonFatalDiagnostics: List<ConeDiagnostic>)

    abstract override fun replaceTypeArguments(newTypeArguments: List<CfirTypeRef>)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirErrorResolvedQualifier

    abstract override fun <D> transformExplicitParent(transformer: CfirTransformer<D>, data: D): CfirErrorResolvedQualifier

    abstract override fun <D> transformTypeArguments(transformer: CfirTransformer<D>, data: D): CfirErrorResolvedQualifier
}
