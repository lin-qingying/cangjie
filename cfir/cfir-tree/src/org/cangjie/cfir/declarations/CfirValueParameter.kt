package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 函数参数声明。
 */
class CfirValueParameter(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    override val status: CfirDeclarationStatus = CfirDeclarationStatus.DEFAULT,
    override val typeParameters: List<CfirTypeParameter> = emptyList(),
    override var returnTypeRef: CfirTypeRef,
    val name: Name,
    /** 默认值表达式 */
    var defaultValue: CfirExpression? = null,
) : CfirCallableDeclaration {
    override val symbol: CfirValueParameterSymbol = CfirValueParameterSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitValueParameter(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirValueParameter {
        returnTypeRef = returnTypeRef.accept(transformer, data) as CfirTypeRef
        defaultValue = defaultValue?.let { it.accept(transformer, data) as CfirExpression }
        return this
    }
}
