

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.enumConstructor]
 */
abstract class CfirEnumConstructor : CfirCallableDeclaration() {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val symbol: CfirEnumConstructorSymbol
    abstract val status: CfirDeclarationStatus
    abstract val typeParameters: List<CfirTypeParameter>
    abstract val returnTypeRef: CfirTypeRef
    abstract val name: Name

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitEnumConstructor(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformEnumConstructor(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    abstract fun replaceStatus(newStatus: CfirDeclarationStatus)


    abstract fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor


    abstract fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor


    abstract fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor


    abstract fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor

}
