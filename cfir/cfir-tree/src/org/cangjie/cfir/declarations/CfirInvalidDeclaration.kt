package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.symbols.CfirInvalidDeclarationSymbol
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 无法识别或暂不支持的声明占位节点。
 */
class CfirInvalidDeclaration(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Synthetic,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    val reason: String,
) : CfirDeclaration {
    override val symbol: CfirInvalidDeclarationSymbol = CfirInvalidDeclarationSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitInvalidDeclaration(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirInvalidDeclaration = this
}
