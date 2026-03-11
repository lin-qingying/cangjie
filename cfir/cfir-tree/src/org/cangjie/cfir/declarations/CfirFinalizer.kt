package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.symbols.CfirFinalizerSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * Finalizer 声明（对应官方编译器中的 FuncDecl 且 identifier 为 "~init"）。
 */
class CfirFinalizer(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    override val status: CfirDeclarationStatus = CfirDeclarationStatus.DEFAULT,
    override val typeParameters: List<CfirTypeParameter> = emptyList(),
    override var returnTypeRef: CfirTypeRef,
    val valueParameters: List<CfirValueParameter> = emptyList(),
    var body: CfirBlock? = null,
) : CfirCallableDeclaration {
    override val symbol: CfirFinalizerSymbol = CfirFinalizerSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFinalizer(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirFinalizer {
        returnTypeRef = returnTypeRef.accept(transformer, data) as CfirTypeRef
        body = body?.let { it.accept(transformer, data) as CfirBlock }
        return this
    }
}
