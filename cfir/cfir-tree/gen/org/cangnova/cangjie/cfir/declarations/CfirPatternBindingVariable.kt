

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
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
abstract class CfirPatternBindingVariable : CfirVariable(), CfirTypeParametersOwner, CfirPatternTreeNode {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val isLocal: Boolean
    abstract override val deprecationsProvider: DeprecationsProvider
    abstract override val dispatchReceiverType: ConeSimpleCangJieType?
    abstract override val status: CfirDeclarationStatus
    abstract override val contentSource: CfirContentSource
    abstract override val isVar: Boolean
    abstract override val isTypeImplicit: Boolean
    abstract override val typeParameters: List<CfirTypeParameter>
    abstract override val symbol: CfirPatternBindingSymbol
    /**
     * 整体查询可读取独立保存槽或宿主模式的整值视图；此属性本身不拥有表达式子树。
     */
    abstract override val initializer: CfirExpression?
    /**
     * 实际拥有的初始化表达式，例如从 CJO 完成产品读取的 RANGE/STRING For 绑定存储槽。
     */
    abstract val ownedInitializer: CfirExpression?
    /**
     * 唯一包含该 binding 的模式树宿主。该反向语义引用不是 CFIR 子节点。
     * 只有宿主为 CfirPatternVariable 且绑定形状匹配时才派生整个 initializer。
     */
    abstract override val patternTreeOwner: CfirPatternTreeOwner?
    abstract override val returnTypeRef: CfirTypeRef
    abstract val name: Name

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        ownedInitializer?.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitPatternBindingVariable(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformPatternBindingVariable(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract override fun replaceDispatchReceiverType(newDispatchReceiverType: ConeSimpleCangJieType?)

    abstract override fun replaceStatus(newStatus: CfirDeclarationStatus)

    abstract override fun replaceInitializer(newInitializer: CfirExpression?)

    abstract fun replaceOwnedInitializer(newOwnedInitializer: CfirExpression?)

    abstract override fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable

    abstract override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable

    abstract override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable

    abstract override fun <D> transformInitializer(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable

    abstract fun <D> transformOwnedInitializer(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable

    abstract override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirPatternBindingVariable
}
