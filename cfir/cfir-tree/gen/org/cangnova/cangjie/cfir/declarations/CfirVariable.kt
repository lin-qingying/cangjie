

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.variable]
 */
sealed class CfirVariable : CfirCallableDeclaration(), CfirStatement {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val typeParameters: List<CfirTypeParameterRef>
    abstract override val isLocal: Boolean
    abstract override val returnTypeRef: CfirTypeRef
    abstract override val deprecationsProvider: DeprecationsProvider
    abstract override val dispatchReceiverType: ConeSimpleCangJieType?
    abstract override val symbol: CfirVariableSymbol<*>
    abstract override val status: CfirDeclarationStatus
    abstract val initializer: CfirExpression?
    abstract val isVar: Boolean

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitVariable(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformVariable(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef)

    abstract override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract override fun replaceStatus(newStatus: CfirDeclarationStatus)

    abstract fun replaceInitializer(newInitializer: CfirExpression?)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirVariable

    abstract override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirVariable

    abstract override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirVariable

    abstract override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirVariable

    abstract fun <D> transformInitializer(transformer: CfirTransformer<D>, data: D): CfirVariable
}
