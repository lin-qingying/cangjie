package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 扩展声明（仓颉特有），对应仓颉编译器中的 ExtendDecl。
 *
 * 仓颉的 extend 可以为已有类型追加接口实现和方法：
 *   extend SomeType <: SomeInterface {
 *       func newMethod() { ... }
 *   }
 */
class CfirExtend(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    override val status: CfirDeclarationStatus = CfirDeclarationStatus.DEFAULT,
    override val typeParameters: List<CfirTypeParameter> = emptyList(),
    /** 被扩展的类型 */
    val extendedTypeRef: CfirTypeRef,
    /** 扩展实现的接口列表（<: 后的接口） */
    override val superTypeRefs: MutableList<CfirTypeRef> = mutableListOf(),
    /** 扩展内部的声明（方法、属性等） */
    override val declarations: MutableList<CfirDeclaration> = mutableListOf(),
) : CfirClassLikeDeclaration {
    override val symbol: CfirExtendSymbol = CfirExtendSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitExtend(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirExtend {
        superTypeRefs.replaceAll { it.accept(transformer, data) as CfirTypeRef }
        declarations.replaceAll { it.accept(transformer, data) as CfirDeclaration }
        return this
    }
}
