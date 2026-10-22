

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeNode
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.patternBindingVariable]
 */
abstract class CfirPatternBindingVariable : CfirVariable(), CfirPatternTreeNode {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val isLocal: Boolean
    abstract override val deprecationsProvider: DeprecationsProvider
    abstract override val dispatchReceiverType: ConeSimpleCangJieType?
    abstract override val status: CfirDeclarationStatus
    abstract override val isVar: Boolean
    abstract override val isTypeImplicit: Boolean
    abstract override val symbol: CfirPatternBindingSymbol
    abstract override val typeParameters: List<CfirTypeParameter>
    /**
     * 仅由唯一 owning CfirPatternVariable 派生的语义 initializer，不拥有表达式子树。
     * 复制 binding 时不会复制该值；它由 binding 的 patternTreeOwner 动态读取。
     */
    abstract override val initializer: CfirExpression?
    /**
     * 唯一包含该 binding 的模式树宿主。该反向语义引用不是 CFIR 子节点。
     * 只有宿主为 CfirPatternVariable 且绑定形状匹配时才派生整个 initializer。
     */
    abstract override val patternTreeOwner: CfirPatternTreeOwner?
    abstract override val returnTypeRef: CfirTypeRef
    abstract val name: Name

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitPatternBindingVariable(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformPatternBindingVariable(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract override fun replaceStatus(newStatus: CfirDeclarationStatus)

    abstract override fun replaceInitializer(newInitializer: CfirExpression?)

    abstract override fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable

    abstract override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable

    abstract override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable

    abstract override fun <D> transformInitializer(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable

    abstract override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable
}
