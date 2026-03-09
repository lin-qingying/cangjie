package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.expressions.CfirStatement
import org.cangjie.cfir.symbols.CfirFileSymbol
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 文件声明，表示一个仓颉源码文件。
 */
class CfirFile(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    /** 文件名 */
    val name: String,
    /** 包声明 */
    val packageDirective: CfirPackageDirective,
    /** 导入列表 */
    val imports: List<CfirImport> = emptyList(),
    /** 顶层声明列表 */
    val declarations: MutableList<CfirDeclaration> = mutableListOf(),
) : CfirDeclaration, CfirStatement {
    override val symbol: CfirFileSymbol = CfirFileSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirFile {
        declarations.replaceAll { it.accept(transformer, data) as CfirDeclaration }
        return this
    }
}
