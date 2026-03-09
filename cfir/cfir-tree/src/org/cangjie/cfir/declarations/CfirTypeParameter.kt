package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 泛型参数声明，对应仓颉编译器中的 GenericParamDecl。
 *
 * 仓颉的泛型参数使用 <: 声明上界约束：
 *   func foo<T <: Comparable<T>>(x: T) { ... }
 */
class CfirTypeParameter(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    val name: Name,
    /** 上界约束列表（通过 <: 约束） */
    val bounds: MutableList<CfirTypeRef> = mutableListOf(),
) : CfirDeclaration {
    override val symbol: CfirTypeParameterSymbol = CfirTypeParameterSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTypeParameter(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypeParameter {
        bounds.replaceAll { it.accept(transformer, data) as CfirTypeRef }
        return this
    }
}
