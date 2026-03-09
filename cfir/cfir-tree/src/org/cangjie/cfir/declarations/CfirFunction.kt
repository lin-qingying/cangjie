package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 函数声明，对应仓颉编译器中的 FuncDecl。
 */
class CfirFunction(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    override val status: CfirDeclarationStatus = CfirDeclarationStatus.DEFAULT,
    override val typeParameters: List<CfirTypeParameter> = emptyList(),
    override var returnTypeRef: CfirTypeRef,
    val name: Name,
    val valueParameters: List<CfirValueParameter> = emptyList(),
    /** 函数体 */
    var body: CfirBlock? = null,
    /** 是否为 mut 函数（仓颉特有） */
    val isMut: Boolean = false,
) : CfirCallableDeclaration {
    override val symbol: CfirFunctionSymbol = CfirFunctionSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFunction(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirFunction {
        returnTypeRef = returnTypeRef.accept(transformer, data) as CfirTypeRef
        body = body?.let { it.accept(transformer, data) as CfirBlock }
        return this
    }
}
