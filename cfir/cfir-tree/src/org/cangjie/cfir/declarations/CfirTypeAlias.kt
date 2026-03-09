package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 类型别名声明，对应仓颉编译器中的 TypeAliasDecl。
 */
class CfirTypeAlias(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    override val status: CfirDeclarationStatus = CfirDeclarationStatus.DEFAULT,
    override val typeParameters: List<CfirTypeParameter> = emptyList(),
    val name: Name,
    /** 别名指向的实际类型 */
    var expandedTypeRef: CfirTypeRef,
) : CfirMemberDeclaration {
    override val symbol: CfirTypeAliasSymbol = CfirTypeAliasSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTypeAlias(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypeAlias {
        expandedTypeRef = expandedTypeRef.accept(transformer, data) as CfirTypeRef
        return this
    }
}
